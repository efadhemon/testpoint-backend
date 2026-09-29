package com.testpoint.attempt;

import com.testpoint.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {
	private final AttemptService attemptService;

	public AttemptController(AttemptService attemptService) {
		this.attemptService = attemptService;
	}

	@GetMapping("/{id}")
	public AttemptDtos.TakeView take(@PathVariable Long id) {
		return attemptService.take(SecurityUtils.currentUser().getId(), id);
	}

	@PutMapping("/{id}/answers")
	public AttemptDtos.SaveResponse save(@PathVariable Long id, @Valid @RequestBody AttemptDtos.SaveRequest request) {
		return attemptService.save(SecurityUtils.currentUser().getId(), id, request);
	}

	@PostMapping("/{id}/submit")
	public AttemptDtos.ResultView submit(@PathVariable Long id, @RequestBody(required = false) AttemptDtos.SaveRequest request) {
		return attemptService.submit(SecurityUtils.currentUser().getId(), id, request);
	}

	@GetMapping("/{id}/result")
	public AttemptDtos.ResultView result(@PathVariable Long id) {
		var user = SecurityUtils.currentUser();
		return attemptService.result(user.getId(), user.getRole(), id);
	}

	@PostMapping("/{id}/ai-summary")
	public AttemptDtos.ResultView summarize(@PathVariable Long id) {
		return attemptService.summarize(SecurityUtils.currentUser().getId(), id);
	}
}
