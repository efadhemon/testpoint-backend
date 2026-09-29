package com.testpoint.attempt;

import com.testpoint.question.QuestionType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class AttemptDtos {
	private AttemptDtos() {
	}

	public record AnswerInput(Long questionId, Long optionId, Boolean booleanAnswer, String textAnswer) {
	}

	public record SaveRequest(List<AnswerInput> answers) {
	}

	public record SaveResponse(boolean submitted, String message) {
	}

	public record TakeOption(Long id, String text) {
	}

	public record TakeAnswer(Long optionId, Boolean booleanAnswer, String textAnswer) {
	}

	public record TakeQuestion(
			Long id,
			int position,
			QuestionType type,
			String text,
			int marks,
			List<TakeOption> options,
			TakeAnswer answer
	) {
	}

	public record TakeView(
			Long id,
			Long quizId,
			String quizTitle,
			Instant expiresAt,
			Instant serverNow,
			AttemptStatus status,
			List<TakeQuestion> questions
	) {
	}

	public record ResultQuestion(
			Long questionId,
			String text,
			QuestionType type,
			int marks,
			Integer awardedMarks,
			Boolean correct,
			String feedback,
			GradeSource gradeSource,
			String yourAnswer,
			String correctAnswer
	) {
	}

	public record ResultView(
			Long id,
			Long quizId,
			String quizTitle,
			AttemptStatus status,
			Integer score,
			Integer maxScore,
			int passingMarks,
			Boolean passed,
			boolean pendingReview,
			String aiSummary,
			Instant submittedAt,
			List<ResultQuestion> questions
	) {
	}

	public record StudentQuizCard(
			Long id,
			String title,
			String instructions,
			int durationMinutes,
			Instant startTime,
			Instant endTime,
			int maxAttempts,
			int attemptsUsed,
			Long inProgressAttemptId,
			String windowState
	) {
	}

	public record StartedAttempt(@NotNull Long attemptId, AttemptStatus status) {
	}
}
