package com.testpoint.ai;

import com.testpoint.question.QuestionDtos;

import java.util.List;

public interface AiClient {
	List<QuestionDtos.QuestionRequest> generateQuestions(String sourceText);

	GradeSuggestion gradeShortAnswer(String question, String rubric, String studentAnswer, int maxMarks);

	String summarize(String prompt);

	record GradeSuggestion(int awardedMarks, String feedback) {
	}
}
