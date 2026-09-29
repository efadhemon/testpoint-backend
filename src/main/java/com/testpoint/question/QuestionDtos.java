package com.testpoint.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class QuestionDtos {
	private QuestionDtos() {
	}

	public record OptionRequest(@NotBlank String text, boolean correct) {
	}

	public record OptionResponse(Long id, String text, boolean correct) {
	}

	public record QuestionRequest(
			@NotNull QuestionType type,
			@NotBlank String text,
			@Min(1) int marks,
			String explanation,
			String modelAnswer,
			Boolean correctBoolean,
			@Valid List<OptionRequest> options
	) {
	}

	public record BulkQuestionRequest(@Valid @Size(min = 1) List<QuestionRequest> questions) {
	}

	public record QuestionResponse(
			Long id,
			QuestionType type,
			String text,
			int marks,
			String explanation,
			String modelAnswer,
			Boolean correctBoolean,
			Instant createdAt,
			List<OptionResponse> options
	) {
		public static QuestionResponse from(Question question) {
			return new QuestionResponse(
					question.getId(),
					question.getType(),
					question.getText(),
					question.getMarks(),
					question.getExplanation(),
					question.getModelAnswer(),
					question.getCorrectBoolean(),
					question.getCreatedAt(),
					question.getOptions().stream()
							.map(option -> new OptionResponse(option.getId(), option.getText(), option.isCorrect()))
							.toList());
		}
	}
}
