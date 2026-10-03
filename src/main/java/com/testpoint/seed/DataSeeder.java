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
import java.util.ArrayList;
import java.util.Collections;
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
		if (userRepository.count() == 0) {
			User instructor = user("Demo Instructor", "instructor@testpoint.local", "Instructor@123", Role.INSTRUCTOR);
			User studentOne = user("Student One", "student1@testpoint.local", "Student@123", Role.STUDENT);
			User studentTwo = user("Student Two", "student2@testpoint.local", "Student@123", Role.STUDENT);
			user("Platform Admin", "admin@testpoint.local", "Admin@123", Role.ADMIN);

			ClassDtos.ClassResponse classGroup = classService.create(instructor.getId(), new ClassDtos.CreateClassRequest("CSE352 Section 03"));
			classService.addStudent(classService.owned(instructor.getId(), classGroup.id()), studentOne);
			classService.addStudent(classService.owned(instructor.getId(), classGroup.id()), studentTwo);
			log.info("Seeded TestPoint accounts. Admin admin@testpoint.local / Admin@123, instructor instructor@testpoint.local / Instructor@123, students student1@testpoint.local and student2@testpoint.local / Student@123");
		}
		seedCourseQuizzes();
	}

	private void seedCourseQuizzes() {
		User instructor = userRepository.findByEmail("instructor@testpoint.local").orElse(null);
		if (instructor == null) {
			return;
		}
		List<ClassDtos.ClassResponse> classes = classService.listForInstructor(instructor.getId());
		if (classes.isEmpty()) {
			return;
		}
		Long classId = classes.stream()
				.filter(item -> "CSE352 Section 03".equals(item.name()))
				.map(ClassDtos.ClassResponse::id)
				.findFirst()
				.orElse(classes.get(0).id());
		var existing = quizService.titles(instructor.getId());
		Instant start = Instant.now().minusSeconds(3600);
		Instant end = Instant.now().plusSeconds(30L * 24 * 3600);
		int added = 0;
		for (CourseQuiz course : courseQuizzes()) {
			if (existing.contains(course.title())) {
				continue;
			}
			List<Long> questionIds = new ArrayList<>();
			for (QuestionDtos.QuestionRequest question : course.questions()) {
				questionIds.add(questionService.create(instructor.getId(), question).id());
			}
			QuizDtos.QuizResponse quiz = quizService.create(instructor.getId(), new QuizDtos.QuizRequest(
					course.title(),
					course.instructions(),
					25,
					start,
					end,
					false,
					3,
					course.passingMarks(),
					questionIds));
			quizService.publish(instructor.getId(), quiz.id());
			quizService.assign(instructor.getId(), quiz.id(), new QuizDtos.AssignRequest(classId, List.of()));
			added++;
		}
		if (added > 0) {
			log.info("Seeded {} Advanced Java quizzes for CSE352 Section 03", added);
		}
	}

	private List<CourseQuiz> courseQuizzes() {
		return List.of(
				new CourseQuiz(
						"Spring Boot",
						"Advanced Java quiz. Multiple-choice and true/false are scored on submit. Short answers wait for review.",
						8,
						List.of(
								mcq("Which stereotype annotation marks a class that owns business rules, such as scoring an attempt?",
										2,
										"@Controller handles HTTP. @Service owns the business rules. @Repository talks to persistence.",
										"@Service",
										"@Controller", "@Repository", "@Configuration"),
								mcq("Where does a Spring Boot app conventionally read its own settings, such as the server port and datasource URL?",
										2,
										"application.properties and application.yml are loaded from the classpath by default.",
										"application.yml or application.properties",
										"web.xml", "persistence.xml", "MANIFEST.MF"),
								tf("A Spring Data JPA repository is an interface. Spring generates the implementation at runtime.",
										true,
										"You declare methods such as findByEmail. Spring Data implements them."),
								mcq("What does a repository method named findByEmail(String email) mean in Spring Data JPA?",
										2,
										"Spring Data derives the query from the method name and the entity fields.",
										"Spring derives a query that selects the entity by its email field",
										"The method must be implemented by hand in a static block",
										"It deletes every row whose email matches",
										"It only works when the entity has no @Id"),
								tf("Setting spring.jpa.hibernate.ddl-auto to update drops the schema and recreates every table on startup.",
										false,
										"update adjusts the schema to match the entities. create and create-drop rebuild it."),
								shortAnswer("Why is constructor injection preferred over field injection with @Autowired?",
										"Constructor injection makes every required dependency visible, lets those fields be final, and lets a unit test pass mocks without starting Spring. Field injection hides dependencies and makes the class harder to construct in a test."))),
				new CourseQuiz(
						"Java Collections",
						"Collections framework quiz. Short answers are reviewed after you submit.",
						8,
						List.of(
								mcq("Which collection rejects duplicate elements and does not keep insertion order?",
										2,
										"HashSet uses hashing. It does not preserve order. LinkedHashSet does.",
										"HashSet",
										"ArrayList", "LinkedList", "ArrayDeque"),
								mcq("Which statement about HashMap and Hashtable is correct?",
										2,
										"HashMap allows one null key and is not synchronized. Hashtable allows no null key and synchronizes each method.",
										"HashMap allows one null key and is not synchronized; Hashtable allows no null key and is synchronized",
										"HashMap is synchronized and Hashtable is not",
										"Both preserve insertion order",
										"Hashtable allows many null keys and HashMap allows none"),
								tf("List.of(1, 2, 3) returns a list that rejects add and remove.",
										true,
										"List.of returns an unmodifiable list."),
								mcq("Which Map keeps entries in the order they were inserted?",
										2,
										"LinkedHashMap records insertion order. TreeMap sorts by key. HashMap does not promise order.",
										"LinkedHashMap",
										"HashMap", "TreeMap", "WeakHashMap"),
								tf("Removing an element from an ArrayList inside a for-each loop over that same list throws ConcurrentModificationException.",
										true,
										"The for-each loop uses a fail-fast iterator. Structural changes must go through that iterator."),
								shortAnswer("When would you choose a TreeSet instead of a HashSet?",
										"Choose a TreeSet when the elements must stay sorted by natural order or a Comparator. Add, contains, and remove are O(log n). Choose a HashSet when you only need uniqueness and average O(1) lookups, and order does not matter."))),
				new CourseQuiz(
						"Java Threading",
						"Threading and concurrency quiz. Short answers are reviewed after you submit.",
						8,
						List.of(
								mcq("What does synchronized on an instance method lock?",
										2,
										"A synchronized instance method locks the current object, this.",
										"The current instance (this)",
										"The Class object for that class",
										"A new lock created for every call",
										"Only the thread that created the object"),
								mcq("Which ExecutorService method schedules a task and returns a Future?",
										2,
										"submit returns a Future. execute returns void. shutdown stops accepting new tasks.",
										"submit",
										"execute", "shutdown", "awaitTermination"),
								tf("Declaring a field volatile makes count++ atomic.",
										false,
										"volatile publishes the write. count++ is still a read-modify-write and needs atomic classes or a lock."),
								mcq("Which description matches a race condition?",
										2,
										"A race condition means the result depends on timing between threads. Deadlock is threads waiting on each other's locks.",
										"The result depends on the order in which threads interleave",
										"Two threads each hold a lock the other needs",
										"A thread never leaves a waiting state",
										"Thread.start throws an exception"),
								tf("Calling thread.run() creates a new thread of execution.",
										false,
										"run() executes on the caller. start() creates the new thread, which then calls run()."),
								shortAnswer("What is a deadlock, and name one way to avoid it.",
										"A deadlock is when two or more threads each hold a lock the others need, so none can continue. Avoid it by acquiring locks in one global order, by using tryLock with a timeout, or by not holding more than one lock at a time."))),
				new CourseQuiz(
						"Java Generics",
						"Generics quiz. Short answers are reviewed after you submit.",
						8,
						List.of(
								mcq("Why does List<String> names = new ArrayList<Object>() fail to compile?",
										2,
										"Generics are invariant. ArrayList<Object> is not a subtype of List<String>.",
										"Generics are invariant, so ArrayList<Object> is not a List<String>",
										"ArrayList cannot store String",
										"Object does not implement Comparable",
										"The diamond operator is required on the left side"),
								mcq("What can you safely do with a List<? extends Number>?",
										2,
										"? extends Number is a producer. You can read Number values. You cannot add a Number, because the list might be a List<Integer>.",
										"Read Number values, but not add a Number",
										"Add any Number, but not read the elements",
										"Add only Integer, and read only Double",
										"Treat it as a raw List with no restrictions"),
								tf("A method may declare its own type parameter, as in <T> T first(List<T> items).",
										true,
										"Generic methods declare type parameters before the return type. They do not need a generic class."),
								mcq("What does type erasure do to a generic type such as List<String>?",
										2,
										"The compiler checks the type arguments, then removes them. At runtime the list is a raw List.",
										"The compiler checks String, then removes the type argument so the runtime type is a raw List",
										"It stores the type argument as a Class field on every list",
										"It deletes the class if the type argument is unused",
										"It turns every String into Object before the program runs"),
								tf("Inside a generic class Box<T>, the expression new T[10] compiles.",
										false,
										"Type erasure removes T, so the runtime cannot allocate a T array."),
								shortAnswer("Explain PECS: when do you use ? extends T, and when do you use ? super T?",
										"? extends T is for a producer. You read T values out and you do not put T values in. ? super T is for a consumer. You put T values in and you only read them as Object. Use extends when the collection supplies values, and super when the collection receives them."))));
	}

	private QuestionDtos.QuestionRequest mcq(String text, int marks, String explanation, String correct, String... wrong) {
		List<String> choices = new ArrayList<>();
		choices.add(correct);
		Collections.addAll(choices, wrong);
		Collections.rotate(choices, Math.floorMod(text.hashCode(), choices.size()));
		List<QuestionDtos.OptionRequest> options = new ArrayList<>();
		for (String choice : choices) {
			options.add(new QuestionDtos.OptionRequest(choice, choice.equals(correct)));
		}
		return new QuestionDtos.QuestionRequest(QuestionType.MCQ, text, marks, explanation, null, null, options);
	}

	private QuestionDtos.QuestionRequest tf(String text, boolean correct, String explanation) {
		return new QuestionDtos.QuestionRequest(QuestionType.TRUE_FALSE, text, 1, explanation, null, correct, List.of());
	}

	private QuestionDtos.QuestionRequest shortAnswer(String text, String modelAnswer) {
		return new QuestionDtos.QuestionRequest(QuestionType.SHORT_ANSWER, text, 5, null, modelAnswer, null, List.of());
	}

	private record CourseQuiz(String title, String instructions, int passingMarks, List<QuestionDtos.QuestionRequest> questions) {
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
