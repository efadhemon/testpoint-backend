package com.testpoint.quiz;

import com.testpoint.question.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class QuizDtos {
	private QuizDtos() {
	}

	public record QuizRequest(
			@NotBlank @Size(max = 180) String title,
			String instructions,
			@Min(1) int durationMinutes,
			@NotNull Instant startTime,
			@NotNull Instant endTime,
			boolean shuffleQuestions,
			@Min(1) int maxAttempts,
			@Min(0) int passingMarks,
			List<Long> questionIds
	) {
	}

	public record AddQuestionRequest(@NotNull Long questionId, Integer marksOverride) {
	}

	public record AssignRequest(Long classId, List<Long> studentIds) {
	}

	public record QuizQuestionResponse(Long questionId, String text, QuestionType type, int marks, int position) {
	}

	public record AssignmentResponse(
			Long id,
			AssignmentTarget targetType,
			Long classId,
			String className,
			Long studentId,
			String studentName
	) {
	}

	public record QuizResponse(
			Long id,
			String title,
			String instructions,
			int durationMinutes,
			Instant startTime,
			Instant endTime,
			boolean shuffleQuestions,
			int maxAttempts,
			QuizStatus status,
			int passingMarks,
			int totalMarks,
			List<QuizQuestionResponse> questions,
			List<AssignmentResponse> assignments
	) {
	}

	public record InstructorSummary(long classCount, long quizCount, long pendingGrades) {
	}
}
