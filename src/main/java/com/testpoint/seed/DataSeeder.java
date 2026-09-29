package com.testpoint.seed;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final ClassService classService;
	private final QuestionService questionService;
	private final QuizService quizService;

	public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, ClassService classService, QuestionService questionService, QuizService quizService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.classService = classService;
		this.questionService = questionService;
		this.quizService = quizService;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			return;
		}
		User admin = user("Platform Admin", "admin@testpoint.local", "Admin@123", Role.ADMIN);
		User instructor = user("Demo Instructor", "instructor@testpoint.local", "Instructor@123", Role.INSTRUCTOR);
		User studentOne = user("Student One", "student1@testpoint.local", "Student@123", Role.STUDENT);
		User studentTwo = user("Student Two", "student2@testpoint.local", "Student@123", Role.STUDENT);

		ClassDtos.ClassResponse classGroup = classService.create(instructor.getId(), new ClassDtos.CreateClassRequest("CSE352 Section 03"));
		classService.addStudent(classService.owned(instructor.getId(), classGroup.id()), studentOne);
		classService.addStudent(classService.owned(instructor.getId(), classGroup.id()), studentTwo);

		var mcq = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.MCQ,
				"Which layer in a Spring Boot app owns business rules such as scoring?",
				2,
				"Controllers handle HTTP. Services own the rules.",
				null,
				null,
				List.of(
						new QuestionDtos.OptionRequest("Controller", false),
						new QuestionDtos.OptionRequest("Service", true),
						new QuestionDtos.OptionRequest("Repository", false),
						new QuestionDtos.OptionRequest("View", false))));
		var truth = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.TRUE_FALSE,
				"Spring Data JPA repositories are interfaces.",
				1,
				"Spring generates the implementation at runtime.",
				null,
				true,
				List.of()));
		var shortAnswer = questionService.create(instructor.getId(), new QuestionDtos.QuestionRequest(
				QuestionType.SHORT_ANSWER,
				"Why should a quiz attempt hide the correct answers until the student submits?",
				5,
				null,
				"Hiding correct answers prevents students from reading the key while the timer is running and keeps the attempt fair.",
				null,
				List.of()));

		Instant start = Instant.now().minusSeconds(3600);
		Instant end = Instant.now().plusSeconds(30L * 24 * 3600);
		QuizDtos.QuizResponse quiz = quizService.create(instructor.getId(), new QuizDtos.QuizRequest(
				"Spring Boot basics",
				"Answer every question. Short answers are reviewed after you submit.",
				20,
				start,
				end,
				true,
				2,
				4,
				List.of(mcq.id(), truth.id(), shortAnswer.id())));
		quizService.publish(instructor.getId(), quiz.id());
		quizService.assign(instructor.getId(), quiz.id(), new QuizDtos.AssignRequest(classGroup.id(), List.of()));
		log.info("Seeded TestPoint. Admin admin@testpoint.local / Admin@123, instructor instructor@testpoint.local / Instructor@123, students student1@testpoint.local and student2@testpoint.local / Student@123");
	}

	private User user(String name, String email, String password, Role role) {
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(role);
		user.setEnabled(true);
		return userRepository.save(user);
	}
}
