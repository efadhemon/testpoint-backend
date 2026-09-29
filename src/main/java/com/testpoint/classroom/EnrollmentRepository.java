package com.testpoint.classroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
	List<Enrollment> findByClassGroupId(Long classId);
	List<Enrollment> findByStudentId(Long studentId);
	Optional<Enrollment> findByClassGroupIdAndStudentId(Long classId, Long studentId);
	long countByClassGroupInstructorId(Long instructorId);
	void deleteByClassGroupId(Long classId);
}
