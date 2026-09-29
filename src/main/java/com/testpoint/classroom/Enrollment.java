package com.testpoint.classroom;

import com.testpoint.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id"}))
public class Enrollment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id")
	private ClassGroup classGroup;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@Column(nullable = false)
	private Instant enrolledAt;

	@PrePersist
	void onCreate() {
		if (enrolledAt == null) {
			enrolledAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public ClassGroup getClassGroup() { return classGroup; }
	public void setClassGroup(ClassGroup classGroup) { this.classGroup = classGroup; }
	public User getStudent() { return student; }
	public void setStudent(User student) { this.student = student; }
	public Instant getEnrolledAt() { return enrolledAt; }
}
