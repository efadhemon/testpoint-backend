package com.testpoint.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserDtos {
	private UserDtos() {
	}

	public record RegisterRequest(
			@NotBlank @Size(max = 120) String name,
			@NotBlank @Email String email,
			@NotBlank @Size(min = 8, max = 100) String password,
			@NotNull Role role
	) {
	}

	public record LoginRequest(
			@NotBlank @Email String email,
			@NotBlank String password
	) {
	}

	public record UserResponse(Long id, String name, String email, Role role, boolean enabled, Instant createdAt) {
		public static UserResponse from(User user) {
			return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
		}
	}

	public record AuthResponse(String token, UserResponse user) {
	}

	public record UpdateUserRequest(Role role, Boolean enabled) {
	}

	public record AdminStats(long users, long instructors, long students, long quizzes, long attempts) {
	}
}
