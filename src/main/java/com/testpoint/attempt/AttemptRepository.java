package com.testpoint.attempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
	List<Attempt> findByStudentIdAndQuizId(Long studentId, Long quizId);

	List<Attempt> findByQuizId(Long quizId);

	List<Attempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

	Optional<Attempt> findByIdAndStudentId(Long id, Long studentId);

	@Query("""
			select distinct a from Attempt a
			left join fetch a.questions
			left join fetch a.quiz
			left join fetch a.student
			where a.id = :id
			""")
	Optional<Attempt> findDetailed(@Param("id") Long id);
}
