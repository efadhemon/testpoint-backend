package com.testpoint.attempt;

import com.testpoint.security.SecurityUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/quizzes")
public class StudentQuizController {
	private final AttemptService attemptService;

	public StudentQuizController(AttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@org.springframework.web.bind.annotation.GetMapping
	public java.util.List<AttemptDtos.StudentQuizCard> list() {
		return attemptService.listForStudent(SecurityUtils.currentUser().getId());
	}

	@PostMapping("/{id}/start")
	public AttemptDtos.StartedAttempt start(@PathVariable Long id) {
		return attemptService.start(SecurityUtils.currentUser().getId(), id);
	}
}
