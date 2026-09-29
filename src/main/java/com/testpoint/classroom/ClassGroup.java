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

import java.time.Instant;

@Entity
@Table(name = "class_groups")
public class ClassGroup {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 140)
	private String name;

	@Column(nullable = false, unique = true, length = 12)
	private String joinCode;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instructor_id")
	private User instructor;

	@Column(nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getJoinCode() { return joinCode; }
	public void setJoinCode(String joinCode) { this.joinCode = joinCode; }
	public User getInstructor() { return instructor; }
	public void setInstructor(User instructor) { this.instructor = instructor; }
	public Instant getCreatedAt() { return createdAt; }
}
