package com.testpoint.classroom;

import com.testpoint.common.ApiException;
import com.testpoint.quiz.QuizAssignmentRepository;
import com.testpoint.user.Role;
import com.testpoint.user.User;
import com.testpoint.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class ClassService {
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private final ClassGroupRepository classGroupRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final QuizAssignmentRepository quizAssignmentRepository;
	private final SecureRandom random = new SecureRandom();

	public ClassService(ClassGroupRepository classGroupRepository, EnrollmentRepository enrollmentRepository, UserRepository userRepository, QuizAssignmentRepository quizAssignmentRepository) {
		this.classGroupRepository = classGroupRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.quizAssignmentRepository = quizAssignmentRepository;
	}

	@Transactional
	public ClassDtos.ClassResponse create(Long instructorId, ClassDtos.CreateClassRequest request) {
		ClassGroup classGroup = new ClassGroup();
		classGroup.setName(request.name().trim());
		classGroup.setJoinCode(uniqueCode());
		classGroup.setInstructor(userRepository.getReferenceById(instructorId));
		classGroupRepository.save(classGroup);
		return toResponse(classGroup, List.of());
	}

	@Transactional(readOnly = true)
	public List<ClassDtos.ClassResponse> listForInstructor(Long instructorId) {
		return classGroupRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId).stream()
				.map(classGroup -> toResponse(classGroup, enrollmentRepository.findByClassGroupId(classGroup.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ClassDtos.ClassResponse> listForStudent(Long studentId) {
		return enrollmentRepository.findByStudentId(studentId).stream()
				.map(enrollment -> toResponse(enrollment.getClassGroup(), List.of()))
				.toList();
	}

	@Transactional
	public ClassDtos.ClassResponse update(Long instructorId, Long classId, ClassDtos.UpdateClassRequest request) {
		ClassGroup classGroup = owned(instructorId, classId);
		classGroup.setName(request.name().trim());
		return toResponse(classGroup, enrollmentRepository.findByClassGroupId(classId));
	}

	@Transactional
	public void delete(Long instructorId, Long classId) {
		owned(instructorId, classId);
		quizAssignmentRepository.deleteByClassGroupId(classId);
		enrollmentRepository.deleteByClassGroupId(classId);
		classGroupRepository.deleteById(classId);
	}

	@Transactional
	public ClassDtos.ClassResponse enroll(Long instructorId, Long classId, ClassDtos.EnrollRequest request) {
		ClassGroup classGroup = owned(instructorId, classId);
		User student = userRepository.findByEmail(request.email().trim().toLowerCase())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "email", "No student account uses that email"));
		if (student.getRole() != Role.STUDENT) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "email", "That account is not a student");
		}
		addStudent(classGroup, student);
		return toResponse(classGroup, enrollmentRepository.findByClassGroupId(classId));
	}

	@Transactional
	public ClassDtos.ClassResponse join(Long studentId, ClassDtos.JoinRequest request) {
		ClassGroup classGroup = classGroupRepository.findByJoinCodeIgnoreCase(request.code().trim())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "code", "No class uses that join code"));
		User student = userRepository.findById(studentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
		addStudent(classGroup, student);
		return toResponse(classGroup, List.of());
	}

	@Transactional
	public void addStudent(ClassGroup classGroup, User student) {
		if (enrollmentRepository.findByClassGroupIdAndStudentId(classGroup.getId(), student.getId()).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "This student is already in the class");
		}
		Enrollment enrollment = new Enrollment();
		enrollment.setClassGroup(classGroup);
		enrollment.setStudent(student);
		enrollmentRepository.save(enrollment);
	}

	@Transactional(readOnly = true)
	public ClassGroup owned(Long instructorId, Long classId) {
		ClassGroup classGroup = classGroupRepository.findById(classId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Class not found"));
		if (!classGroup.getInstructor().getId().equals(instructorId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You do not teach this class");
		}
		return classGroup;
	}

	private ClassDtos.ClassResponse toResponse(ClassGroup classGroup, List<Enrollment> enrollments) {
		List<ClassDtos.StudentResponse> students = enrollments.stream()
				.map(enrollment -> new ClassDtos.StudentResponse(
						enrollment.getStudent().getId(),
						enrollment.getStudent().getName(),
						enrollment.getStudent().getEmail()))
				.toList();
		return new ClassDtos.ClassResponse(
				classGroup.getId(),
				classGroup.getName(),
				classGroup.getJoinCode(),
				classGroup.getCreatedAt(),
				students.size(),
				students);
	}

	private String uniqueCode() {
		for (int attempt = 0; attempt < 20; attempt++) {
			StringBuilder code = new StringBuilder(6);
			for (int i = 0; i < 6; i++) {
				code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
			}
			if (!classGroupRepository.existsByJoinCode(code.toString())) {
				return code.toString();
			}
		}
		throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate a join code");
	}
}
