package com.testpoint.question;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_options")
public class QuestionOption {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	@Column(nullable = false, columnDefinition = "text")
	private String text;

	@Column(nullable = false)
	private boolean correct;

	@Column(nullable = false)
	private int position;

	public Long getId() { return id; }
	public Question getQuestion() { return question; }
	public void setQuestion(Question question) { this.question = question; }
	public String getText() { return text; }
	public void setText(String text) { this.text = text; }
	public boolean isCorrect() { return correct; }
	public void setCorrect(boolean correct) { this.correct = correct; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
}
