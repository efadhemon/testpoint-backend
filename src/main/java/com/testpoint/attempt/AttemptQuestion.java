package com.testpoint.attempt;

import com.testpoint.question.QuestionType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "attempt_questions")
public class AttemptQuestion {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_id")
	private Attempt attempt;

	@Column(nullable = false)
	private Long sourceQuestionId;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false)
	private int marks;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuestionType type;

	@Column(nullable = false, columnDefinition = "text")
	private String text;

	@Column(columnDefinition = "text")
	private String explanation;

	@Column(columnDefinition = "text")
	private String modelAnswer;

	private Boolean correctBoolean;

	@OneToMany(mappedBy = "attemptQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position asc")
	private List<AttemptOption> options = new ArrayList<>();

	@OneToOne(mappedBy = "attemptQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
	private AttemptAnswer answer;

	public Long getId() { return id; }
	public Attempt getAttempt() { return attempt; }
	public void setAttempt(Attempt attempt) { this.attempt = attempt; }
	public Long getSourceQuestionId() { return sourceQuestionId; }
	public void setSourceQuestionId(Long sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public int getMarks() { return marks; }
	public void setMarks(int marks) { this.marks = marks; }
	public QuestionType getType() { return type; }
	public void setType(QuestionType type) { this.type = type; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
	public String getExplanation() { return explanation; }
	public void setExplanation(String explanation) { this.explanation = explanation; }
	public String getModelAnswer() { return modelAnswer; }
	public void setModelAnswer(String modelAnswer) { this.modelAnswer = modelAnswer; }
	public Boolean getCorrectBoolean() { return correctBoolean; }
	public void setCorrectBoolean(Boolean correctBoolean) { this.correctBoolean = correctBoolean; }
	public List<AttemptOption> getOptions() { return options; }
	public AttemptAnswer getAnswer() { return answer; }
	public void setAnswer(AttemptAnswer answer) { this.answer = answer; }
}
