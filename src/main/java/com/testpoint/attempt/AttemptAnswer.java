package com.testpoint.attempt;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attempt_question_id", unique = true)
	private AttemptQuestion attemptQuestion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "selected_option_id")
	private AttemptOption selectedOption;

	private Boolean booleanAnswer;

	@Column(columnDefinition = "text")
	private String textAnswer;

	private Boolean correct;
	private Integer awardedMarks;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private GradeSource gradeSource;

	@Column(columnDefinition = "text")
	private String feedback;

	private Instant gradedAt;

	public Long getId() { return id; }
	public AttemptQuestion getAttemptQuestion() { return attemptQuestion; }
	public void setAttemptQuestion(AttemptQuestion attemptQuestion) { this.attemptQuestion = attemptQuestion; }
	public AttemptOption getSelectedOption() { return selectedOption; }
	public void setSelectedOption(AttemptOption selectedOption) { this.selectedOption = selectedOption; }
	public Boolean getBooleanAnswer() { return booleanAnswer; }
	public void setBooleanAnswer(Boolean booleanAnswer) { this.booleanAnswer = booleanAnswer; }
	public String getTextAnswer() { return textAnswer; }
	public void setTextAnswer(String textAnswer) { this.textAnswer = textAnswer; }
	public Boolean getCorrect() { return correct; }
	public void setCorrect(Boolean correct) { this.correct = correct; }
	public Integer getAwardedMarks() { return awardedMarks; }
	public void setAwardedMarks(Integer awardedMarks) { this.awardedMarks = awardedMarks; }
	public GradeSource getGradeSource() { return gradeSource; }
	public void setGradeSource(GradeSource gradeSource) { this.gradeSource = gradeSource; }
	public String getFeedback() { return feedback; }
	public void setFeedback(String feedback) { this.feedback = feedback; }
	public Instant getGradedAt() { return gradedAt; }
	public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
}
