package com.testpoint.user;

import com.testpoint.attempt.AttemptRepository;
import com.testpoint.quiz.QuizRepository;
import com.testpoint.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
	private final UserService userService;
	private final UserRepository userRepository;
	private final QuizRepository quizRepository;
	private final AttemptRepository attemptRepository;

	public AdminController(UserService userService, UserRepository userRepository, QuizRepository quizRepository, AttemptRepository attemptRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.quizRepository = quizRepository;
		this.attemptRepository = attemptRepository;
	}

	@GetMapping("/users")
	public List<UserDtos.UserResponse> users() {
		return userService.listUsers();
	}

	@PatchMapping("/users/{id}")
	public UserDtos.UserResponse update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest request) {
		return userService.updateUser(SecurityUtils.currentUser().getId(), id, request);
	}

	@GetMapping("/stats")
	public UserDtos.AdminStats stats() {
		return new UserDtos.AdminStats(
				userRepository.count(),
				userRepository.countByRole(Role.INSTRUCTOR),
				userRepository.countByRole(Role.STUDENT),
				quizRepository.count(),
				attemptRepository.count());
	}
}
