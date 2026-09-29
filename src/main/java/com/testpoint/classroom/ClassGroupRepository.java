package com.testpoint.classroom;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
	List<ClassGroup> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);
	Optional<ClassGroup> findByJoinCodeIgnoreCase(String joinCode);
	boolean existsByJoinCode(String joinCode);
}
