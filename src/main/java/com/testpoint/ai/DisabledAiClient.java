package com.testpoint.ai;

import com.testpoint.common.ApiException;
import com.testpoint.question.QuestionDtos;
import org.springframework.http.HttpStatus;

import java.util.List;

public class DisabledAiClient implements AiClient {
	@Override
	public List<QuestionDtos.QuestionRequest> generateQuestions(String sourceText) {
		throw unavailable();
	}

	@Override
	public GradeSuggestion gradeShortAnswer(String question, String rubric, String studentAnswer, int maxMarks) {
		throw unavailable();
	}

	@Override
	public String summarize(String prompt) {
		throw unavailable();
	}

	private ApiException unavailable() {
		return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI is not configured. Set AI_ENABLED and AI_API_KEY in backend/.env");
	}
}
