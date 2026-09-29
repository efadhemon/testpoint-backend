package com.testpoint.attempt;

import com.testpoint.ai.AiClient;
import com.testpoint.classroom.EnrollmentRepository;
import com.testpoint.common.ApiException;
import com.testpoint.question.QuestionType;
import com.testpoint.quiz.Quiz;
import com.testpoint.quiz.QuizAssignmentRepository;
import com.testpoint.quiz.QuizQuestion;
import com.testpoint.quiz.QuizRepository;
import com.testpoint.quiz.QuizStatus;
import com.testpoint.user.Role;
import com.testpoint.user.User;
import com.testpoint.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class AttemptService {
	private final AttemptRepository attemptRepository;
	private final AttemptAnswerRepository answerRepository;
	private final QuizRepository quizRepository;
	private final QuizAssignmentRepository assignmentRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final AiClient aiClient;

	public AttemptService(
			AttemptRepository attemptRepository,
			AttemptAnswerRepository answerRepository,
			QuizRepository quizRepository,
			QuizAssignmentRepository assignmentRepository,
			EnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			AiClient aiClient) {
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.quizRepository = quizRepository;
		this.assignmentRepository = assignmentRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.aiClient = aiClient;
	}

	@Transactional
	public List<AttemptDtos.StudentQuizCard> listForStudent(Long studentId) {
		Instant now = Instant.now();
		List<AttemptDtos.StudentQuizCard> cards = new ArrayList<>();
		for (Quiz quiz : assignmentRepository.findPublishedForStudent(studentId)) {
			quiz.getQuestions().size();
			List<Attempt> attempts = attemptRepository.findByStudentIdAndQuizId(studentId, quiz.getId());
			for (Attempt attempt : attempts) {
				expireIfNeeded(attempt);
			}
			Attempt inProgress = attempts.stream().filter(item -> item.getStatus() == AttemptStatus.IN_PROGRESS).findFirst().orElse(null);
			int used = (int) attempts.stream().filter(item -> item.getStatus() != AttemptStatus.IN_PROGRESS).count();
			String window = now.isBefore(quiz.getStartTime()) ? "UPCOMING" : now.isAfter(quiz.getEndTime()) ? "CLOSED" : "OPEN";
			cards.add(new AttemptDtos.StudentQuizCard(
					quiz.getId(),
					quiz.getTitle(),
					quiz.getInstructions(),
					quiz.getDurationMinutes(),
					quiz.getStartTime(),
					quiz.getEndTime(),
					quiz.getMaxAttempts(),
					used,
					inProgress == null ? null : inProgress.getId(),
					window));
		}
		return cards;
	}

	@Transactional
	public AttemptDtos.StartedAttempt start(Long studentId, Long quizId) {
		Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
		if (quiz.getStatus() != QuizStatus.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "This quiz is not open");
		}
		if (!isAssigned(studentId, quizId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "This quiz is not assigned to you");
		}
		Instant now = Instant.now();
		if (now.isBefore(quiz.getStartTime())) {
			throw new ApiException(HttpStatus.CONFLICT, "This quiz has not started yet");
		}
		if (now.isAfter(quiz.getEndTime())) {
			throw new ApiException(HttpStatus.CONFLICT, "This quiz is closed");
		}
		List<Attempt> existing = attemptRepository.findByStudentIdAndQuizId(studentId, quizId);
		for (Attempt attempt : existing) {
			expireIfNeeded(attempt);
		}
		Attempt current = existing.stream().filter(item -> item.getStatus() == AttemptStatus.IN_PROGRESS).findFirst().orElse(null);
		if (current != null) {
			return new AttemptDtos.StartedAttempt(current.getId(), current.getStatus());
		}
		long used = existing.stream().filter(item -> item.getStatus() != AttemptStatus.IN_PROGRESS).count();
		if (used >= quiz.getMaxAttempts()) {
			throw new ApiException(HttpStatus.CONFLICT, "You have used every attempt for this quiz");
		}
		Instant expiresAt = now.plusSeconds(quiz.getDurationMinutes() * 60L);
		if (quiz.getEndTime().isBefore(expiresAt)) {
			expiresAt = quiz.getEndTime();
		}
		if (!expiresAt.isAfter(now)) {
			throw new ApiException(HttpStatus.CONFLICT, "There is no time left to start this quiz");
		}
		Attempt attempt = new Attempt();
		attempt.setStudent(userRepository.getReferenceById(studentId));
		attempt.setQuiz(quiz);
		attempt.setStartedAt(now);
		attempt.setExpiresAt(expiresAt);
		attempt.setStatus(AttemptStatus.IN_PROGRESS);
		List<QuizQuestion> questions = new ArrayList<>(quiz.getQuestions());
		if (quiz.isShuffleQuestions()) {
			Collections.shuffle(questions);
		}
		int position = 1;
		for (QuizQuestion link : questions) {
			AttemptQuestion snapshot = new AttemptQuestion();
			snapshot.setAttempt(attempt);
			snapshot.setSourceQuestionId(link.getQuestion().getId());
			snapshot.setPosition(position++);
			snapshot.setMarks(link.marks());
			snapshot.setType(link.getQuestion().getType());
			snapshot.setText(link.getQuestion().getText());
			snapshot.setExplanation(link.getQuestion().getExplanation());
			snapshot.setModelAnswer(link.getQuestion().getModelAnswer());
			snapshot.setCorrectBoolean(link.getQuestion().getCorrectBoolean());
			int optionPosition = 1;
			for (var option : link.getQuestion().getOptions()) {
				AttemptOption copy = new AttemptOption();
				copy.setAttemptQuestion(snapshot);
				copy.setSourceOptionId(option.getId());
				copy.setText(option.getText());
				copy.setCorrect(option.isCorrect());
				copy.setPosition(optionPosition++);
				snapshot.getOptions().add(copy);
			}
			attempt.getQuestions().add(snapshot);
		}
		attemptRepository.save(attempt);
		return new AttemptDtos.StartedAttempt(attempt.getId(), attempt.getStatus());
	}

	@Transactional
	public AttemptDtos.TakeView take(Long studentId, Long attemptId) {
		Attempt attempt = owned(studentId, attemptId);
		if (expireIfNeeded(attempt).getStatus() != AttemptStatus.IN_PROGRESS) {
			throw new ApiException(HttpStatus.CONFLICT, "This attempt is already submitted");
		}
		return toTakeView(attempt);
	}

	@Transactional
	public AttemptDtos.SaveResponse save(Long studentId, Long attemptId, AttemptDtos.SaveRequest request) {
		Attempt attempt = owned(studentId, attemptId);
		if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			throw new ApiException(HttpStatus.CONFLICT, "This attempt is already submitted");
		}
		applyAnswers(attempt, request);
		if (!Instant.now().isBefore(attempt.getExpiresAt())) {
			finish(attempt);
			return new AttemptDtos.SaveResponse(true, "Time is up. Your attempt was submitted.");
		}
		return new AttemptDtos.SaveResponse(false, "Saved");
	}

	@Transactional
	public AttemptDtos.ResultView submit(Long studentId, Long attemptId, AttemptDtos.SaveRequest request) {
		Attempt attempt = owned(studentId, attemptId);
		if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
			return toResult(attempt);
		}
		applyAnswers(attempt, request);
		finish(attempt);
		return toResult(attempt);
	}

	@Transactional(readOnly = true)
	public AttemptDtos.ResultView result(Long userId, Role role, Long attemptId) {
		Attempt attempt = readable(userId, role, attemptId);
		if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
			throw new ApiException(HttpStatus.CONFLICT, "Submit the attempt before viewing results");
		}
		return toResult(attempt);
	}

	@Transactional
	public AttemptDtos.ResultView summarize(Long studentId, Long attemptId) {
		Attempt attempt = owned(studentId, attemptId);
		if (attempt.getStatus() != AttemptStatus.GRADED) {
			throw new ApiException(HttpStatus.CONFLICT, "A summary is available after every answer is graded");
		}
		if (attempt.getAiSummary() == null || attempt.getAiSummary().isBlank()) {
			StringBuilder prompt = new StringBuilder();
			prompt.append("Write a short, encouraging study summary for a student. Mention what they understood and what to review.\n");
			prompt.append("Quiz: ").append(attempt.getQuiz().getTitle()).append('\n');
			prompt.append("Score: ").append(attempt.getScore()).append(" / ").append(attempt.getMaxScore()).append('\n');
			for (AttemptQuestion question : attempt.getQuestions()) {
				AttemptAnswer answer = question.getAnswer();
				prompt.append("- ").append(question.getText())
						.append(" awarded ").append(answer == null ? 0 : answer.getAwardedMarks())
						.append("/").append(question.getMarks()).append('\n');
			}
			attempt.setAiSummary(aiClient.summarize(prompt.toString()));
		}
		return toResult(attempt);
	}

	@Transactional(readOnly = true)
	public List<PendingAnswer> pending(Long instructorId) {
		return answerRepository.findPendingForInstructor(instructorId).stream().map(PendingAnswer::from).toList();
	}

	@Transactional
	public PendingAnswer grade(Long instructorId, Long answerId, int awardedMarks, String feedback) {
		AttemptAnswer answer = answerRepository.findById(answerId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Answer not found"));
		Attempt attempt = answer.getAttemptQuestion().getAttempt();
		if (!attempt.getQuiz().getInstructor().getId().equals(instructorId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You do not grade this quiz");
		}
		if (answer.getAttemptQuestion().getType() != QuestionType.SHORT_ANSWER) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Only short answers are graded by hand");
		}
		applyManualGrade(answer, awardedMarks, feedback, GradeSource.MANUAL);
		recalculate(attempt);
		return PendingAnswer.from(answer);
	}

	@Transactional
	public AttemptDtos.ResultView gradeWithAi(Long instructorId, Long attemptId) {
		Attempt attempt = attemptRepository.findDetailed(attemptId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
		loadChildren(attempt);
		if (!attempt.getQuiz().getInstructor().getId().equals(instructorId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You do not grade this quiz");
		}
		for (AttemptQuestion question : attempt.getQuestions()) {
			if (question.getType() != QuestionType.SHORT_ANSWER) {
				continue;
			}
			AttemptAnswer answer = question.getAnswer();
			if (answer == null || answer.getAwardedMarks() != null) {
				continue;
			}
			String studentText = answer.getTextAnswer() == null ? "" : answer.getTextAnswer();
			AiClient.GradeSuggestion suggestion = aiClient.gradeShortAnswer(
					question.getText(),
					question.getModelAnswer() == null ? "" : question.getModelAnswer(),
					studentText,
					question.getMarks());
			applyManualGrade(answer, suggestion.awardedMarks(), suggestion.feedback(), GradeSource.AI);
		}
		recalculate(attempt);
		return toResult(attempt);
	}

	private boolean isAssigned(Long studentId, Long quizId) {
		return assignmentRepository.findPublishedForStudent(studentId).stream().anyMatch(quiz -> quiz.getId().equals(quizId));
	}

	private Attempt expireIfNeeded(Attempt attempt) {
		if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && !Instant.now().isBefore(attempt.getExpiresAt())) {
			Attempt detailed = attempt.getQuestions().isEmpty()
					? attemptRepository.findDetailed(attempt.getId()).orElse(attempt)
					: attempt;
			loadChildren(detailed);
			finish(detailed);
			return detailed;
		}
		return attempt;
	}

	private void applyAnswers(Attempt attempt, AttemptDtos.SaveRequest request) {
		if (request == null || request.answers() == null) {
			return;
		}
		for (AttemptDtos.AnswerInput input : request.answers()) {
			AttemptQuestion question = attempt.getQuestions().stream()
					.filter(item -> item.getId().equals(input.questionId()))
					.findFirst()
					.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "questionId", "That question is not part of this attempt"));
			AttemptAnswer answer = ensureAnswer(question);
			answer.setSelectedOption(null);
			answer.setBooleanAnswer(null);
			answer.setTextAnswer(null);
			if (question.getType() == QuestionType.MCQ && input.optionId() != null) {
				AttemptOption option = question.getOptions().stream()
						.filter(item -> item.getId().equals(input.optionId()))
						.findFirst()
						.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "optionId", "That option is not part of this question"));
				answer.setSelectedOption(option);
			} else if (question.getType() == QuestionType.TRUE_FALSE) {
				answer.setBooleanAnswer(input.booleanAnswer());
			} else if (question.getType() == QuestionType.SHORT_ANSWER) {
				answer.setTextAnswer(input.textAnswer() == null ? null : input.textAnswer().trim());
			}
		}
	}

	private void finish(Attempt attempt) {
		loadChildren(attempt);
		for (AttemptQuestion question : attempt.getQuestions()) {
			AttemptAnswer answer = ensureAnswer(question);
			if (question.getType() == QuestionType.MCQ) {
				boolean correct = answer.getSelectedOption() != null && answer.getSelectedOption().isCorrect();
				markObjective(answer, correct, question.getMarks(), question.getExplanation());
			} else if (question.getType() == QuestionType.TRUE_FALSE) {
				boolean correct = answer.getBooleanAnswer() != null && Objects.equals(answer.getBooleanAnswer(), question.getCorrectBoolean());
				markObjective(answer, correct, question.getMarks(), question.getExplanation());
			} else if (answer.getAwardedMarks() == null) {
				if (answer.getTextAnswer() == null || answer.getTextAnswer().isBlank()) {
					answer.setAwardedMarks(0);
					answer.setCorrect(false);
					answer.setGradeSource(GradeSource.AUTO);
					answer.setFeedback("No answer was submitted.");
					answer.setGradedAt(Instant.now());
				}
			}
		}
		attempt.setSubmittedAt(Instant.now());
		recalculate(attempt);
	}

	private void markObjective(AttemptAnswer answer, boolean correct, int marks, String explanation) {
		answer.setCorrect(correct);
		answer.setAwardedMarks(correct ? marks : 0);
		answer.setGradeSource(GradeSource.AUTO);
		answer.setGradedAt(Instant.now());
		if (explanation != null && !explanation.isBlank()) {
			answer.setFeedback(explanation);
		} else {
			answer.setFeedback(correct ? "Correct." : "Incorrect.");
		}
	}

	private void applyManualGrade(AttemptAnswer answer, int awardedMarks, String feedback, GradeSource source) {
		int max = answer.getAttemptQuestion().getMarks();
		if (awardedMarks < 0 || awardedMarks > max) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "awardedMarks", "Marks must be between 0 and " + max);
		}
		answer.setAwardedMarks(awardedMarks);
		answer.setCorrect(awardedMarks == max);
		answer.setFeedback(feedback == null || feedback.isBlank() ? null : feedback.trim());
		answer.setGradeSource(source);
		answer.setGradedAt(Instant.now());
	}

	private void recalculate(Attempt attempt) {
		int score = 0;
		int max = 0;
		boolean pending = false;
		for (AttemptQuestion question : attempt.getQuestions()) {
			max += question.getMarks();
			AttemptAnswer answer = question.getAnswer();
			if (answer == null || answer.getAwardedMarks() == null) {
				pending = true;
			} else {
				score += answer.getAwardedMarks();
			}
		}
		attempt.setScore(score);
		attempt.setMaxScore(max);
		attempt.setStatus(pending ? AttemptStatus.SUBMITTED : AttemptStatus.GRADED);
	}

	private AttemptAnswer ensureAnswer(AttemptQuestion question) {
		if (question.getAnswer() == null) {
			AttemptAnswer answer = new AttemptAnswer();
			answer.setAttemptQuestion(question);
			question.setAnswer(answer);
		}
		return question.getAnswer();
	}

	private Attempt owned(Long studentId, Long attemptId) {
		Attempt attempt = attemptRepository.findDetailed(attemptId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
		if (!attempt.getStudent().getId().equals(studentId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "This attempt belongs to another student");
		}
		loadChildren(attempt);
		return attempt;
	}

	private Attempt readable(Long userId, Role role, Long attemptId) {
		Attempt attempt = attemptRepository.findDetailed(attemptId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attempt not found"));
		loadChildren(attempt);
		boolean owner = attempt.getStudent().getId().equals(userId);
		boolean instructor = role == Role.INSTRUCTOR && attempt.getQuiz().getInstructor().getId().equals(userId);
		boolean admin = role == Role.ADMIN;
		if (!owner && !instructor && !admin) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You cannot view this result");
		}
		return attempt;
	}

	private void loadChildren(Attempt attempt) {
		for (AttemptQuestion question : attempt.getQuestions()) {
			question.getOptions().size();
			if (question.getAnswer() != null && question.getAnswer().getSelectedOption() != null) {
				question.getAnswer().getSelectedOption().getText();
			}
		}
	}

	private AttemptDtos.TakeView toTakeView(Attempt attempt) {
		List<AttemptDtos.TakeQuestion> questions = attempt.getQuestions().stream().map(question -> {
			AttemptAnswer answer = question.getAnswer();
			return new AttemptDtos.TakeQuestion(
					question.getId(),
					question.getPosition(),
					question.getType(),
					question.getText(),
					question.getMarks(),
					question.getOptions().stream().map(option -> new AttemptDtos.TakeOption(option.getId(), option.getText())).toList(),
					new AttemptDtos.TakeAnswer(
							answer == null || answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId(),
							answer == null ? null : answer.getBooleanAnswer(),
							answer == null ? null : answer.getTextAnswer()));
		}).toList();
		return new AttemptDtos.TakeView(
				attempt.getId(),
				attempt.getQuiz().getId(),
				attempt.getQuiz().getTitle(),
				attempt.getExpiresAt(),
				Instant.now(),
				attempt.getStatus(),
				questions);
	}

	private AttemptDtos.ResultView toResult(Attempt attempt) {
		List<AttemptDtos.ResultQuestion> questions = attempt.getQuestions().stream().map(question -> {
			AttemptAnswer answer = question.getAnswer();
			boolean revealed = answer != null && answer.getAwardedMarks() != null;
			return new AttemptDtos.ResultQuestion(
					question.getSourceQuestionId(),
					question.getText(),
					question.getType(),
					question.getMarks(),
					answer == null ? null : answer.getAwardedMarks(),
					revealed ? answer.getCorrect() : null,
					revealed ? answer.getFeedback() : null,
					revealed ? answer.getGradeSource() : null,
					yourAnswer(question, answer),
					revealed ? correctAnswer(question) : null);
		}).toList();
		boolean pending = attempt.getStatus() != AttemptStatus.GRADED;
		Boolean passed = attempt.getStatus() == AttemptStatus.GRADED
				? attempt.getScore() != null && attempt.getScore() >= attempt.getQuiz().getPassingMarks()
				: null;
		return new AttemptDtos.ResultView(
				attempt.getId(),
				attempt.getQuiz().getId(),
				attempt.getQuiz().getTitle(),
				attempt.getStatus(),
				attempt.getScore(),
				attempt.getMaxScore(),
				attempt.getQuiz().getPassingMarks(),
				passed,
				pending,
				attempt.getAiSummary(),
				attempt.getSubmittedAt(),
				questions);
	}

	private String yourAnswer(AttemptQuestion question, AttemptAnswer answer) {
		if (answer == null) {
			return "No answer";
		}
		if (question.getType() == QuestionType.MCQ) {
			return answer.getSelectedOption() == null ? "No answer" : answer.getSelectedOption().getText();
		}
		if (question.getType() == QuestionType.TRUE_FALSE) {
			if (answer.getBooleanAnswer() == null) {
				return "No answer";
			}
			return answer.getBooleanAnswer() ? "True" : "False";
		}
		return answer.getTextAnswer() == null || answer.getTextAnswer().isBlank() ? "No answer" : answer.getTextAnswer();
	}

	private String correctAnswer(AttemptQuestion question) {
		if (question.getType() == QuestionType.MCQ) {
			return question.getOptions().stream().filter(AttemptOption::isCorrect).map(AttemptOption::getText).findFirst().orElse(null);
		}
		if (question.getType() == QuestionType.TRUE_FALSE) {
			return Boolean.TRUE.equals(question.getCorrectBoolean()) ? "True" : "False";
		}
		return question.getModelAnswer();
	}

	public record PendingAnswer(
			Long answerId,
			Long attemptId,
			Long quizId,
			String quizTitle,
			String studentName,
			String questionText,
			String modelAnswer,
			String textAnswer,
			int marks
	) {
		static PendingAnswer from(AttemptAnswer answer) {
			AttemptQuestion question = answer.getAttemptQuestion();
			Attempt attempt = question.getAttempt();
			return new PendingAnswer(
					answer.getId(),
					attempt.getId(),
					attempt.getQuiz().getId(),
					attempt.getQuiz().getTitle(),
					attempt.getStudent().getName(),
					question.getText(),
					question.getModelAnswer(),
					answer.getTextAnswer(),
					question.getMarks());
		}
	}
}
