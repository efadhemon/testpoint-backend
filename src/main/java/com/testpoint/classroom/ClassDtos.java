package com.testpoint.classroom;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ClassDtos {
	private ClassDtos() {
	}

	public record CreateClassRequest(@NotBlank @Size(max = 140) String name) {
	}

	public record UpdateClassRequest(@NotBlank @Size(max = 140) String name) {
	}

	public record EnrollRequest(@NotBlank @Email String email) {
	}

	public record JoinRequest(@NotBlank String code) {
	}

	public record StudentResponse(Long id, String name, String email) {
	}

	public record ClassResponse(
			Long id,
			String name,
			String joinCode,
			Instant createdAt,
			int studentCount,
			List<StudentResponse> students
	) {
	}
}
