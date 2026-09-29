package com.testpoint.attempt;

import com.testpoint.quiz.Quiz;
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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attempts")
public class Attempt {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id")
	private User student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id")
	private Quiz quiz;

	@Column(nullable = false)
	private Instant startedAt;

	private Instant submittedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AttemptStatus status = AttemptStatus.IN_PROGRESS;

	private Integer score;
	private Integer maxScore;

	@Column(columnDefinition = "text")
	private String aiSummary;

	@OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position asc")
	private List<AttemptQuestion> questions = new ArrayList<>();

	public Long getId() { return id; }
	public User getStudent() { return student; }
	public void setStudent(User student) { this.student = student; }
	public Quiz getQuiz() { return quiz; }
	public void setQuiz(Quiz quiz) { this.quiz = quiz; }
	public Instant getStartedAt() { return startedAt; }
	public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
	public Instant getSubmittedAt() { return submittedAt; }
	public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
	public AttemptStatus getStatus() { return status; }
	public void setStatus(AttemptStatus status) { this.status = status; }
	public Integer getScore() { return score; }
	public void setScore(Integer score) { this.score = score; }
	public Integer getMaxScore() { return maxScore; }
	public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }
	public String getAiSummary() { return aiSummary; }
	public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
	public List<AttemptQuestion> getQuestions() { return questions; }
}
