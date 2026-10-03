package com.testpoint.quiz;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
	Page<Quiz> findByInstructorId(Long instructorId, Pageable pageable);

	List<Quiz> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);

	long countByInstructorId(Long instructorId);
}
