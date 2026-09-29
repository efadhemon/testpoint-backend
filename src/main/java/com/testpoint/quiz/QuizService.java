package com.testpoint.quiz;

import com.testpoint.attempt.AttemptAnswerRepository;
import com.testpoint.attempt.AttemptRepository;
import com.testpoint.classroom.ClassGroupRepository;
import com.testpoint.classroom.ClassService;
import com.testpoint.classroom.EnrollmentRepository;
import com.testpoint.common.ApiException;
import com.testpoint.question.Question;
import com.testpoint.question.QuestionService;
import com.testpoint.user.Role;
import com.testpoint.user.User;
import com.testpoint.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {
	private final QuizRepository quizRepository;
	private final QuizAssignmentRepository assignmentRepository;
	private final AttemptRepository attemptRepository;
	private final AttemptAnswerRepository answerRepository;
	private final QuestionService questionService;
	private final ClassService classService;
	private final ClassGroupRepository classGroupRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;

	public QuizService(
			QuizRepository quizRepository,
			QuizAssignmentRepository assignmentRepository,
			AttemptRepository attemptRepository,
			AttemptAnswerRepository answerRepository,
			QuestionService questionService,
			ClassService classService,
			ClassGroupRepository classGroupRepository,
			EnrollmentRepository enrollmentRepository,
			UserRepository userRepository) {
		this.quizRepository = quizRepository;
		this.assignmentRepository = assignmentRepository;
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.questionService = questionService;
		this.classService = classService;
		this.classGroupRepository = classGroupRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public QuizDtos.QuizResponse create(Long instructorId, QuizDtos.QuizRequest request) {
		Quiz quiz = new Quiz();
		quiz.setInstructor(userRepository.getReferenceById(instructorId));
		quiz.setStatus(QuizStatus.DRAFT);
		apply(quiz, instructorId, request);
		quizRepository.save(quiz);
		return toResponse(quiz);
	}

	@Transactional(readOnly = true)
	public List<QuizDtos.QuizResponse> list(Long instructorId) {
		return quizRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public QuizDtos.QuizResponse get(Long instructorId, Long quizId) {
		return toResponse(owned(instructorId, quizId));
	}

	@Transactional
	public QuizDtos.QuizResponse update(Long instructorId, Long quizId, QuizDtos.QuizRequest request) {
		Quiz quiz = owned(instructorId, quizId);
		requireDraft(quiz);
		apply(quiz, instructorId, request);
		return toResponse(quiz);
	}

	@Transactional
	public QuizDtos.QuizResponse addQuestion(Long instructorId, Long quizId, QuizDtos.AddQuestionRequest request) {
		Quiz quiz = owned(instructorId, quizId);
		requireDraft(quiz);
		Question question = questionService.owned(instructorId, request.questionId());
		boolean exists = quiz.getQuestions().stream().anyMatch(item -> item.getQuestion().getId().equals(question.getId()));
		if (exists) {
			throw new ApiException(HttpStatus.CONFLICT, "That question is already on this quiz");
		}
		QuizQuestion link = new QuizQuestion();
		link.setQuiz(quiz);
		link.setQuestion(question);
		link.setPosition(quiz.getQuestions().size() + 1);
		link.setMarksOverride(request.marksOverride());
		quiz.getQuestions().add(link);
		return toResponse(quiz);
	}

	@Transactional
	public QuizDtos.QuizResponse removeQuestion(Long instructorId, Long quizId, Long questionId) {
		Quiz quiz = owned(instructorId, quizId);
		requireDraft(quiz);
		quiz.getQuestions().removeIf(item -> item.getQuestion().getId().equals(questionId));
		int position = 1;
		for (QuizQuestion item : quiz.getQuestions()) {
			item.setPosition(position++);
		}
		return toResponse(quiz);
	}

	@Transactional
	public QuizDtos.QuizResponse publish(Long instructorId, Long quizId) {
		Quiz quiz = owned(instructorId, quizId);
		requireDraft(quiz);
		validateReady(quiz);
		quiz.setStatus(QuizStatus.PUBLISHED);
		return toResponse(quiz);
	}

	@Transactional
	public QuizDtos.QuizResponse close(Long instructorId, Long quizId) {
		Quiz quiz = owned(instructorId, quizId);
		if (quiz.getStatus() != QuizStatus.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "Only a published quiz can be closed");
		}
		quiz.setStatus(QuizStatus.CLOSED);
		return toResponse(quiz);
	}

	@Transactional
	public QuizDtos.QuizResponse assign(Long instructorId, Long quizId, QuizDtos.AssignRequest request) {
		Quiz quiz = owned(instructorId, quizId);
		if (quiz.getStatus() != QuizStatus.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "Publish the quiz before assigning it");
		}
		boolean added = false;
		if (request.classId() != null) {
			var classGroup = classService.owned(instructorId, request.classId());
			boolean exists = assignmentRepository.findByQuizId(quizId).stream()
					.anyMatch(item -> item.getClassGroup() != null && item.getClassGroup().getId().equals(classGroup.getId()));
			if (!exists) {
				QuizAssignment assignment = new QuizAssignment();
				assignment.setQuiz(quiz);
				assignment.setTargetType(AssignmentTarget.CLASS);
				assignment.setClassGroup(classGroup);
				assignmentRepository.save(assignment);
				added = true;
			}
		}
		if (request.studentIds() != null) {
			for (Long studentId : request.studentIds()) {
				User student = userRepository.findById(studentId)
						.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student not found"));
				if (student.getRole() != Role.STUDENT) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Only student accounts can be assigned");
				}
				boolean enrolled = enrollmentRepository.findByStudentId(studentId).stream()
						.anyMatch(enrollment -> enrollment.getClassGroup().getInstructor().getId().equals(instructorId));
				if (!enrolled) {
					throw new ApiException(HttpStatus.BAD_REQUEST, student.getName() + " is not in one of your classes");
				}
				boolean exists = assignmentRepository.findByQuizId(quizId).stream()
						.anyMatch(item -> item.getStudent() != null && item.getStudent().getId().equals(studentId));
				if (!exists) {
					QuizAssignment assignment = new QuizAssignment();
					assignment.setQuiz(quiz);
					assignment.setTargetType(AssignmentTarget.STUDENT);
					assignment.setStudent(student);
					assignmentRepository.save(assignment);
					added = true;
				}
			}
		}
		if (!added && request.classId() == null && (request.studentIds() == null || request.studentIds().isEmpty())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a class or at least one student");
		}
		return toResponse(quiz);
	}

	@Transactional
	public void delete(Long instructorId, Long quizId) {
		Quiz quiz = owned(instructorId, quizId);
		if (!attemptRepository.findByQuizId(quizId).isEmpty()) {
			throw new ApiException(HttpStatus.CONFLICT, "This quiz already has attempts and cannot be deleted");
		}
		assignmentRepository.deleteAll(assignmentRepository.findByQuizId(quizId));
		quizRepository.delete(quiz);
	}

	@Transactional(readOnly = true)
	public QuizDtos.InstructorSummary summary(Long instructorId) {
		return new QuizDtos.InstructorSummary(
				classGroupRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).size(),
				quizRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).size(),
				answerRepository.findPendingForInstructor(instructorId).size());
	}

	public Quiz owned(Long instructorId, Long quizId) {
		Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Quiz not found"));
		if (!quiz.getInstructor().getId().equals(instructorId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this quiz");
		}
		quiz.getQuestions().size();
		return quiz;
	}

	private void apply(Quiz quiz, Long instructorId, QuizDtos.QuizRequest request) {
		if (!request.startTime().isBefore(request.endTime())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "endTime", "The end time must be after the start time");
		}
		quiz.setTitle(request.title().trim());
		quiz.setInstructions(request.instructions() == null ? null : request.instructions().trim());
		quiz.setDurationMinutes(request.durationMinutes());
		quiz.setStartTime(request.startTime());
		quiz.setEndTime(request.endTime());
		quiz.setShuffleQuestions(request.shuffleQuestions());
		quiz.setMaxAttempts(request.maxAttempts());
		quiz.setPassingMarks(request.passingMarks());
		if (request.questionIds() != null) {
			replaceQuestions(quiz, instructorId, request.questionIds());
		}
	}

	private void replaceQuestions(Quiz quiz, Long instructorId, List<Long> questionIds) {
		quiz.getQuestions().clear();
		int position = 1;
		for (Long questionId : questionIds) {
			Question question = questionService.owned(instructorId, questionId);
			QuizQuestion link = new QuizQuestion();
			link.setQuiz(quiz);
			link.setQuestion(question);
			link.setPosition(position++);
			quiz.getQuestions().add(link);
		}
	}

	private void validateReady(Quiz quiz) {
		if (quiz.getQuestions().isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Add at least one question before publishing");
		}
		if (quiz.getStartTime() == null || quiz.getEndTime() == null || !quiz.getStartTime().isBefore(quiz.getEndTime())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "endTime", "Set a valid start and end window");
		}
		if (quiz.getDurationMinutes() < 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "durationMinutes", "Duration must be at least 1 minute");
		}
		int total = quiz.getQuestions().stream().mapToInt(QuizQuestion::marks).sum();
		if (quiz.getPassingMarks() > total) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "passingMarks", "Passing marks cannot exceed the quiz total");
		}
	}

	private void requireDraft(Quiz quiz) {
		if (quiz.getStatus() != QuizStatus.DRAFT) {
			throw new ApiException(HttpStatus.CONFLICT, "Only a draft quiz can be edited");
		}
	}

	private QuizDtos.QuizResponse toResponse(Quiz quiz) {
		List<QuizDtos.QuizQuestionResponse> questions = quiz.getQuestions().stream()
				.map(item -> new QuizDtos.QuizQuestionResponse(
						item.getQuestion().getId(),
						item.getQuestion().getText(),
						item.getQuestion().getType(),
						item.marks(),
						item.getPosition()))
				.toList();
		List<QuizDtos.AssignmentResponse> assignments = assignmentRepository.findByQuizId(quiz.getId()).stream()
				.map(item -> new QuizDtos.AssignmentResponse(
						item.getId(),
						item.getTargetType(),
						item.getClassGroup() == null ? null : item.getClassGroup().getId(),
						item.getClassGroup() == null ? null : item.getClassGroup().getName(),
						item.getStudent() == null ? null : item.getStudent().getId(),
						item.getStudent() == null ? null : item.getStudent().getName()))
				.toList();
		int total = questions.stream().mapToInt(QuizDtos.QuizQuestionResponse::marks).sum();
		return new QuizDtos.QuizResponse(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getInstructions(),
				quiz.getDurationMinutes(),
				quiz.getStartTime(),
				quiz.getEndTime(),
				quiz.isShuffleQuestions(),
				quiz.getMaxAttempts(),
				quiz.getStatus(),
				quiz.getPassingMarks(),
				total,
				questions,
				assignments);
	}
}
