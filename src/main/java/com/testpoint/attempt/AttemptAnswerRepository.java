package com.testpoint.attempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {
	@Query("""
			select ans from AttemptAnswer ans
			join fetch ans.attemptQuestion q
			join fetch q.attempt a
			join fetch a.quiz quiz
			join fetch a.student student
			where quiz.instructor.id = :instructorId
			and a.status = com.testpoint.attempt.AttemptStatus.SUBMITTED
			and q.type = com.testpoint.question.QuestionType.SHORT_ANSWER
			and ans.awardedMarks is null
			""")
	List<AttemptAnswer> findPendingForInstructor(@Param("instructorId") Long instructorId);
}
