package com.testpoint.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, Long> {
	List<QuizAssignment> findByQuizId(Long quizId);

	void deleteByClassGroupId(Long classId);

	@Query("""
			select distinct q from Quiz q
			join QuizAssignment a on a.quiz = q
			where q.status = com.testpoint.quiz.QuizStatus.PUBLISHED
			and (
				a.student.id = :studentId
				or a.classGroup.id in (
					select e.classGroup.id from Enrollment e where e.student.id = :studentId
				)
			)
			order by q.startTime asc
			""")
	List<Quiz> findPublishedForStudent(@Param("studentId") Long studentId);
}
