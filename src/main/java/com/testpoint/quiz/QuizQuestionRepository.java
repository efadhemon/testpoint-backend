package com.testpoint.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
	boolean existsByQuestion_Id(Long questionId);
}
