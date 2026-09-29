package com.testpoint.analytics;

import com.testpoint.attempt.AttemptStatus;

import java.time.Instant;
import java.util.List;

public final class AnalyticsDtos {
	private AnalyticsDtos() {
	}

	public record QuestionStat(Long questionId, String text, double accuracyPercent, int responses) {
	}

	public record QuizAnalytics(
			Long quizId,
			String title,
			int attemptCount,
			Double averagePercent,
			Double passRate,
			List<QuestionStat> questions
	) {
	}

	public record HistoryItem(
			Long attemptId,
			Long quizId,
			String quizTitle,
			Integer score,
			Integer maxScore,
			AttemptStatus status,
			Instant submittedAt
	) {
	}

	public record StudentAnalytics(int attemptCount, Double averagePercent, List<HistoryItem> history) {
	}
}
