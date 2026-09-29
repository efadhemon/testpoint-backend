package com.testpoint.analytics;

import com.testpoint.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping("/quizzes/{id}")
	public AnalyticsDtos.QuizAnalytics quiz(@PathVariable Long id) {
		return analyticsService.quiz(SecurityUtils.currentUser().getId(), id);
	}

	@GetMapping("/me")
	public AnalyticsDtos.StudentAnalytics me() {
		return analyticsService.me(SecurityUtils.currentUser().getId());
	}
}
