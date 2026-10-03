package com.testpoint.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpoint.common.ApiException;
import com.testpoint.config.AppProperties;
import com.testpoint.question.QuestionDtos;
import com.testpoint.question.QuestionType;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpenAiCompatibleClient implements AiClient {
	private final AppProperties.Ai settings;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

	public OpenAiCompatibleClient(AppProperties.Ai settings, ObjectMapper objectMapper) {
		this.settings = settings;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<QuestionDtos.QuestionRequest> generateQuestions(String sourceText) {
		String prompt = """
				Create 4 to 8 quiz questions from the lecture text.
				Use a mix of MCQ, TRUE_FALSE, and SHORT_ANSWER.
				Return JSON only with this shape:
				{"questions":[{"type":"MCQ","text":"...","marks":1,"explanation":"...","modelAnswer":null,"correctBoolean":null,"options":[{"text":"...","correct":true}]}]}
				MCQ needs 4 options and exactly one correct option.
				TRUE_FALSE sets correctBoolean and options to an empty array.
				SHORT_ANSWER sets modelAnswer to a concise rubric and options to an empty array.
				Lecture text:
				""" + sourceText;
		JsonNode root = ask(prompt);
		JsonNode questions = root.path("questions");
		List<QuestionDtos.QuestionRequest> drafts = new ArrayList<>();
		if (!questions.isArray()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI response did not include questions");
		}
		for (JsonNode node : questions) {
			try {
				QuestionType type = QuestionType.valueOf(node.path("type").asText());
				List<QuestionDtos.OptionRequest> options = new ArrayList<>();
				for (JsonNode option : node.path("options")) {
					options.add(new QuestionDtos.OptionRequest(option.path("text").asText(""), option.path("correct").asBoolean(false)));
				}
				Boolean correctBoolean = node.path("correctBoolean").isNull() || node.path("correctBoolean").isMissingNode()
						? null
						: node.path("correctBoolean").asBoolean();
				String modelAnswer = node.path("modelAnswer").isNull() ? null : node.path("modelAnswer").asText(null);
				drafts.add(new QuestionDtos.QuestionRequest(
						type,
						node.path("text").asText(""),
						Math.max(1, node.path("marks").asInt(1)),
						textOrNull(node, "explanation"),
						modelAnswer,
						correctBoolean,
						options));
			} catch (IllegalArgumentException ignored) {
				// Skip a draft the model labeled with an unknown type.
			}
		}
		if (drafts.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI response did not include usable questions");
		}
		return drafts;
	}

	@Override
	public GradeSuggestion gradeShortAnswer(String question, String rubric, String studentAnswer, int maxMarks) {
		String prompt = """
				Grade this short answer. Award an integer from 0 to %d.
				Return JSON only: {"awardedMarks":0,"feedback":"..."}
				Question: %s
				Rubric: %s
				Student answer: %s
				""".formatted(maxMarks, question, rubric, studentAnswer);
		JsonNode root = ask(prompt);
		int awarded = Math.max(0, Math.min(maxMarks, root.path("awardedMarks").asInt(0)));
		String feedback = root.path("feedback").asText("Reviewed by AI.");
		return new GradeSuggestion(awarded, feedback);
	}

	@Override
	public String summarize(String prompt) {
		JsonNode root = ask(prompt + "\nReturn JSON only: {\"summary\":\"...\"}");
		String summary = root.path("summary").asText("");
		if (summary.isBlank()) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI response did not include a summary");
		}
		return summary;
	}

	private JsonNode ask(String prompt) {
		try {
			String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
					.put("model", settings.model())
					.put("temperature", 0.2)
					.set("messages", objectMapper.createArrayNode()
							.add(objectMapper.createObjectNode().put("role", "system").put("content", "You reply with JSON only."))
							.add(objectMapper.createObjectNode().put("role", "user").put("content", prompt))));
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(trimSlash(settings.baseUrl()) + "/chat/completions"))
					.timeout(Duration.ofSeconds(60))
					.header("Authorization", "Bearer " + settings.apiKey())
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 300) {
				throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI service returned an error: " + upstreamMessage(response.body()));
			}
			JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
			return objectMapper.readTree(extractJson(content.asText()));
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not read the AI response");
		}
	}

	private String upstreamMessage(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			// The OpenAI-compatible Gemini endpoint wraps errors in a one-element array.
			JsonNode node = root.isArray() && !root.isEmpty() ? root.get(0) : root;
			JsonNode error = node.path("error");
			String message = error.path("message").asText("");
			if (message.isBlank() && error.isTextual()) {
				message = error.asText("");
			}
			if (message.isBlank()) {
				message = node.path("message").asText("");
			}
			message = message.replaceAll("\\s+", " ").trim();
			if (!message.isBlank()) {
				return message.length() > 240 ? message.substring(0, 240) : message;
			}
		} catch (Exception ignored) {
			// Fall through to a generic status when the body is not JSON.
		}
		return "the AI service rejected the request";
	}

	private String extractJson(String content) {
		int start = content.indexOf('{');
		int end = content.lastIndexOf('}');
		if (start < 0 || end <= start) {
			throw new ApiException(HttpStatus.BAD_GATEWAY, "The AI response was not JSON");
		}
		return content.substring(start, end + 1);
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
			return null;
		}
		return value.asText();
	}

	private String trimSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
