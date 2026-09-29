package com.testpoint.question;

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
@Table(name = "questions")
public class Question {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instructor_id")
	private User instructor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuestionType type;

	@Column(nullable = false, columnDefinition = "text")
	private String text;

	@Column(nullable = false)
	private int marks;

	@Column(columnDefinition = "text")
	private String explanation;

	@Column(columnDefinition = "text")
	private String modelAnswer;

	private Boolean correctBoolean;

	@Column(nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position asc")
	private List<QuestionOption> options = new ArrayList<>();

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public User getInstructor() { return instructor; }
	public void setInstructor(User instructor) { this.instructor = instructor; }
	public QuestionType getType() { return type; }
	public void setType(QuestionType type) { this.type = type; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
	public int getMarks() { return marks; }
	public void setMarks(int marks) { this.marks = marks; }
	public String getExplanation() { return explanation; }
	public void setExplanation(String explanation) { this.explanation = explanation; }
	public String getModelAnswer() { return modelAnswer; }
	public void setModelAnswer(String modelAnswer) { this.modelAnswer = modelAnswer; }
	public Boolean getCorrectBoolean() { return correctBoolean; }
	public void setCorrectBoolean(Boolean correctBoolean) { this.correctBoolean = correctBoolean; }
	public Instant getCreatedAt() { return createdAt; }
	public List<QuestionOption> getOptions() { return options; }
}
