package com.testpoint.grading;

import com.testpoint.attempt.AttemptDtos;
import com.testpoint.attempt.AttemptService;
import com.testpoint.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grading")
public class GradingController {
	private final AttemptService attemptService;

	public GradingController(AttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@GetMapping("/pending")
	public List<AttemptService.PendingAnswer> pending() {
		return attemptService.pending(SecurityUtils.currentUser().getId());
	}

	@PostMapping("/answers/{id}")
	public AttemptService.PendingAnswer grade(@PathVariable Long id, @Valid @RequestBody GradeRequest request) {
		return attemptService.grade(SecurityUtils.currentUser().getId(), id, request.awardedMarks(), request.feedback());
	}

	@PostMapping("/attempts/{id}/ai")
	public AttemptDtos.ResultView gradeWithAi(@PathVariable Long id) {
		return attemptService.gradeWithAi(SecurityUtils.currentUser().getId(), id);
	}

	public record GradeRequest(@Min(0) int awardedMarks, @Size(max = 2000) String feedback) {
	}
}
