package com.testpoint.quiz;

import com.testpoint.classroom.ClassGroup;
import com.testpoint.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "quiz_assignments")
public class QuizAssignment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id")
	private Quiz quiz;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AssignmentTarget targetType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id")
	private ClassGroup classGroup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(nullable = false)
	private Instant assignedAt;

	@PrePersist
	void onCreate() {
		if (assignedAt == null) {
			assignedAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public Quiz getQuiz() { return quiz; }
	public void setQuiz(Quiz quiz) { this.quiz = quiz; }
	public AssignmentTarget getTargetType() { return targetType; }
	public void setTargetType(AssignmentTarget targetType) { this.targetType = targetType; }
	public ClassGroup getClassGroup() { return classGroup; }
	public void setClassGroup(ClassGroup classGroup) { this.classGroup = classGroup; }
	public User getStudent() { return student; }
	public void setStudent(User student) { this.student = student; }
	public Instant getAssignedAt() { return assignedAt; }
}
