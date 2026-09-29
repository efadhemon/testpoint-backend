package com.testpoint.quiz;

import com.testpoint.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor")
public class InstructorController {
	private final QuizService quizService;

	public InstructorController(QuizService quizService) {
		this.quizService = quizService;
	}

	@GetMapping("/summary")
	public QuizDtos.InstructorSummary summary() {
		return quizService.summary(SecurityUtils.currentUser().getId());
	}
}
