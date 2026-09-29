package com.testpoint;

import com.testpoint.attempt.Attempt;
import com.testpoint.attempt.AttemptDtos;
import com.testpoint.attempt.AttemptRepository;
import com.testpoint.attempt.AttemptService;
import com.testpoint.attempt.AttemptStatus;
import com.testpoint.classroom.ClassDtos;
import com.testpoint.classroom.ClassService;
import com.testpoint.question.QuestionDtos;
import com.testpoint.question.QuestionService;
import com.testpoint.question.QuestionType;
import com.testpoint.quiz.QuizDtos;
import com.testpoint.quiz.QuizService;
import com.testpoint.user.Role;
import com.testpoint.user.User;
import com.testpoint.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttemptEvaluationTest {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private ClassService classService;
	@Autowired
	private QuestionService questionService;
	@Autowired
	private QuizService quizService;
	@Autowired
	private AttemptService attemptService;
	@Autowired
	private AttemptRepository attemptRepository;

	@Test
	void objectiveAnswersAreGradedAndShortAnswersStayPending() {
		User instructor = user("Instructor", "instructor@example.com", Role.INSTRUCTOR);
		User student = user("Student", "student@example.com", Role.STUDENT);
		ClassDtos.ClassResponse classGroup = classService.create(instructor.getId(), new ClassDtos.CreateClassRequest("Lab"));
		classService.addStudent(classService.owned(instructor.getId(), classGroup.id()), student);

		QuestionDtos.QuestionResponse mcq = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.MCQ, "Pick the service layer", 2, "Services own rules.", null, null,
				List.of(new QuestionDtos.OptionRequest("Controller", false), new QuestionDtos.OptionRequest("Service", true))));
		QuestionDtos.QuestionResponse truth = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.TRUE_FALSE, "Repositories are classes.", 1, null, null, false, List.of()));
		QuestionDtos.QuestionResponse written = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.SHORT_ANSWER, "Why hide answers?", 5, null, "To keep the attempt fair.", null, List.of()));

		Long quizId = publish(instructor.getId(), classGroup.id(), List.of(mcq.id(), truth.id(), written.id()), 20);
		Long attemptId = attemptService.start(student.getId(), quizId).attemptId();
		AttemptDtos.TakeView take = attemptService.take(student.getId(), attemptId);
		Long mcqQuestionId = take.questions().get(0).type() == QuestionType.MCQ ? take.questions().get(0).id() : idOf(take, QuestionType.MCQ);
		Long tfQuestionId = idOf(take, QuestionType.TRUE_FALSE);
		Long shortQuestionId = idOf(take, QuestionType.SHORT_ANSWER);
		Long correctOption = take.questions().stream()
				.filter(question -> question.id().equals(mcqQuestionId))
				.flatMap(question -> question.options().stream())
				.filter(option -> option.text().equals("Service"))
				.findFirst().orElseThrow().id();

		AttemptDtos.ResultView result = attemptService.submit(student.getId(), attemptId, new AttemptDtos.SaveRequest(List.of(
				new AttemptDtos.AnswerInput(mcqQuestionId, correctOption, null, null),
				new AttemptDtos.AnswerInput(tfQuestionId, null, true, null),
				new AttemptDtos.AnswerInput(shortQuestionId, null, null, "So nobody can copy the key."))));

		assertEquals(2, result.score());
		assertEquals(8, result.maxScore());
		assertEquals(AttemptStatus.SUBMITTED, result.status());
		assertTrue(result.pendingReview());
		assertNull(result.questions().stream().filter(item -> item.type() == QuestionType.SHORT_ANSWER).findFirst().orElseThrow().awardedMarks());
		assertEquals(Boolean.TRUE, result.questions().stream().filter(item -> item.type() == QuestionType.MCQ).findFirst().orElseThrow().correct());
		assertEquals(Boolean.FALSE, result.questions().stream().filter(item -> item.type() == QuestionType.TRUE_FALSE).findFirst().orElseThrow().correct());

		var pending = attemptService.pending(instructor.getId());
		assertEquals(1, pending.size());
		attemptService.grade(instructor.getId(), pending.get(0).answerId(), 5, "Covers the fairness point.");
		AttemptDtos.ResultView graded = attemptService.result(student.getId(), Role.STUDENT, attemptId);
		assertEquals(AttemptStatus.GRADED, graded.status());
		assertEquals(7, graded.score());
		assertEquals(Boolean.TRUE, graded.passed());
	}

	@Test
	void expiredAttemptIsSubmittedOnSave() {
		User instructor = user("Instructor Two", "instructor2@example.com", Role.INSTRUCTOR);
		User student = user("Student Two", "student2@example.com", Role.STUDENT);
		ClassDtos.ClassResponse classGroup = classService.create(instructor.getId(), new ClassDtos.CreateClassRequest("Timer"));
		classService.addStudent(classService.owned(instructor.getId(), classGroup.id()), student);
		QuestionDtos.QuestionResponse mcq = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.MCQ, "One plus one?", 1, null, null, null,
				List.of(new QuestionDtos.OptionRequest("2", true), new QuestionDtos.OptionRequest("3", false))));
		Long quizId = publish(instructor.getId(), classGroup.id(), List.of(mcq.id()), 15);
		Long attemptId = attemptService.start(student.getId(), quizId).attemptId();
		Attempt attempt = attemptRepository.findById(attemptId).orElseThrow();
		attempt.setExpiresAt(Instant.now().minusSeconds(5));
		attemptRepository.saveAndFlush(attempt);

		AttemptDtos.SaveResponse saved = attemptService.save(student.getId(), attemptId, new AttemptDtos.SaveRequest(List.of()));
		assertTrue(saved.submitted());
		AttemptDtos.ResultView result = attemptService.result(student.getId(), Role.STUDENT, attemptId);
		assertEquals(AttemptStatus.GRADED, result.status());
		assertEquals(0, result.score());
	}

	private Long publish(Long instructorId, Long classId, List<Long> questionIds, int duration) {
		QuizDtos.QuizResponse quiz = quizService.create(instructorId, new QuizDtos.QuizRequest(
				"Quiz", "Instructions", duration, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600),
				false, 1, 1, questionIds));
		quizService.publish(instructorId, quiz.id());
		quizService.assign(instructorId, quiz.id(), new QuizDtos.AssignRequest(classId, List.of()));
		return quiz.id();
	}

	private User user(String name, String email, Role role) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode("password123"));
		user.setRole(role);
		user.setEnabled(true);
		return userRepository.save(user);
	}

	private Long idOf(AttemptDtos.TakeView take, QuestionType type) {
		return take.questions().stream().filter(question -> question.type() == type).findFirst().orElseThrow().id();
	}
}
