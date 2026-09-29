package com.testpoint.quiz;

import com.testpoint.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(columnDefinition = "text")
	private String instructions;

	@Column(nullable = false)
	private int durationMinutes;

	private Instant startTime;
	private Instant endTime;

	@Column(nullable = false)
	private boolean shuffleQuestions;

	@Column(nullable = false)
	private int maxAttempts = 1;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuizStatus status = QuizStatus.DRAFT;

	@Column(nullable = false)
	private int passingMarks;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instructor_id")
	private User instructor;

	@Column(nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position asc")
	private List<QuizQuestion> questions = new ArrayList<>();

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getInstructions() { return instructions; }
	public void setInstructions(String instructions) { this.instructions = instructions; }
	public int getDurationMinutes() { return durationMinutes; }
	public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
	public Instant getStartTime() { return startTime; }
	public void setStartTime(Instant startTime) { this.startTime = startTime; }
	public Instant getEndTime() { return endTime; }
	public void setEndTime(Instant endTime) { this.endTime = endTime; }
	public boolean isShuffleQuestions() { return shuffleQuestions; }
	public void setShuffleQuestions(boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
	public QuizStatus getStatus() { return status; }
	public void setStatus(QuizStatus status) { this.status = status; }
	public int getPassingMarks() { return passingMarks; }
	public void setPassingMarks(int passingMarks) { this.passingMarks = passingMarks; }
	public User getInstructor() { return instructor; }
	public void setInstructor(User instructor) { this.instructor = instructor; }
	public Instant getCreatedAt() { return createdAt; }
	public List<QuizQuestion> getQuestions() { return questions; }
}
