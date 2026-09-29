package com.testpoint.analytics;

import com.testpoint.attempt.Attempt;
import com.testpoint.attempt.AttemptAnswer;
import com.testpoint.attempt.AttemptQuestion;
import com.testpoint.attempt.AttemptRepository;
import com.testpoint.attempt.AttemptStatus;
import com.testpoint.common.ApiException;
import com.testpoint.question.QuestionType;
import com.testpoint.quiz.Quiz;
import com.testpoint.quiz.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {
	private final QuizRepository quizRepository;
	private final AttemptRepository attemptRepository;

	public AnalyticsService(QuizRepository quizRepository, AttemptRepository attemptRepository) {
		this.quizRepository = quizRepository;
		this.attemptRepository = attemptRepository;
	}

	@Transactional(readOnly = true)
	public AnalyticsDtos.QuizAnalytics quiz(Long instructorId, Long quizId) {
		Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
		if (!quiz.getInstructor().getId().equals(instructorId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this quiz");
		}
		List<Attempt> attempts = attemptRepository.findByQuizId(quizId).stream()
				.filter(attempt -> attempt.getStatus() != AttemptStatus.IN_PROGRESS)
				.toList();
		Double average = average(attempts);
		List<Attempt> graded = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.GRADED).toList();
		Double passRate = graded.isEmpty() ? null : graded.stream()
				.filter(attempt -> attempt.getScore() != null && attempt.getScore() >= quiz.getPassingMarks())
				.count() * 100.0 / graded.size();
		Map<Long, Stat> stats = new LinkedHashMap<>();
		for (Attempt attempt : attempts) {
			for (AttemptQuestion question : attempt.getQuestions()) {
				Stat stat = stats.computeIfAbsent(question.getSourceQuestionId(), id -> new Stat(question.getText()));
				AttemptAnswer answer = question.getAnswer();
				if (answer == null || answer.getAwardedMarks() == null) {
					continue;
				}
				stat.responses++;
				boolean accurate = question.getType() == QuestionType.SHORT_ANSWER
						? answer.getAwardedMarks() == question.getMarks()
						: Boolean.TRUE.equals(answer.getCorrect());
				if (accurate) {
					stat.accurate++;
				}
			}
		}
		List<AnalyticsDtos.QuestionStat> questions = new ArrayList<>();
		stats.forEach((id, stat) -> questions.add(new AnalyticsDtos.QuestionStat(
				id,
				stat.text,
				stat.responses == 0 ? 0 : stat.accurate * 100.0 / stat.responses,
				stat.responses)));
		return new AnalyticsDtos.QuizAnalytics(quiz.getId(), quiz.getTitle(), attempts.size(), average, passRate, questions);
	}

	@Transactional(readOnly = true)
	public AnalyticsDtos.StudentAnalytics me(Long studentId) {
		List<Attempt> attempts = attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
		List<Attempt> finished = attempts.stream().filter(attempt -> attempt.getStatus() != AttemptStatus.IN_PROGRESS).toList();
		List<AnalyticsDtos.HistoryItem> history = attempts.stream()
				.map(attempt -> new AnalyticsDtos.HistoryItem(
						attempt.getId(),
						attempt.getQuiz().getId(),
						attempt.getQuiz().getTitle(),
						attempt.getScore(),
						attempt.getMaxScore(),
						attempt.getStatus(),
						attempt.getSubmittedAt()))
				.toList();
		return new AnalyticsDtos.StudentAnalytics(finished.size(), average(finished), history);
	}

	private Double average(List<Attempt> attempts) {
		List<Attempt> scored = attempts.stream()
				.filter(attempt -> attempt.getMaxScore() != null && attempt.getMaxScore() > 0 && attempt.getScore() != null)
				.toList();
		if (scored.isEmpty()) {
			return null;
		}
		return scored.stream().mapToDouble(attempt -> attempt.getScore() * 100.0 / attempt.getMaxScore()).average().orElse(0);
	}

	private static final class Stat {
		private final String text;
		private int responses;
		private int accurate;

		private Stat(String text) {
			this.text = text;
		}
	}
}
