package com.testpoint.quiz;

import com.testpoint.question.Question;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quiz_id")
	private Quiz quiz;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "question_id")
	private Question question;

	private int position;
	private Integer marksOverride;

	public Long getId() { return id; }
	public Quiz getQuiz() { return quiz; }
	public void setQuiz(Quiz quiz) { this.quiz = quiz; }
	public Question getQuestion() { return question; }
	public void setQuestion(Question question) { this.question = question; }
	public int getPosition() { return position; }
	public void setPosition(int position) { this.position = position; }
	public Integer getMarksOverride() { return marksOverride; }
	public void setMarksOverride(Integer marksOverride) { this.marksOverride = marksOverride; }

	public int marks() {
		return marksOverride != null ? marksOverride : question.getMarks();
	}
}
