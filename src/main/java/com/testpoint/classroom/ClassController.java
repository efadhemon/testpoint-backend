package com.testpoint.classroom;

import com.testpoint.security.SecurityUtils;
import com.testpoint.user.Role;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/classes")
public class ClassController {
	private final ClassService classService;

	public ClassController(ClassService classService) {
		this.classService = classService;
	}

	@GetMapping
	public List<ClassDtos.ClassResponse> list() {
		var user = SecurityUtils.currentUser();
		if (user.getRole() == Role.STUDENT) {
			return classService.listForStudent(user.getId());
		}
		if (user.getRole() == Role.INSTRUCTOR) {
			return classService.listForInstructor(user.getId());
		}
		return List.of();
	}

	@PostMapping
	@ResponseStatus(CREATED)
	public ClassDtos.ClassResponse create(@Valid @RequestBody ClassDtos.CreateClassRequest request) {
		requireInstructor();
		return classService.create(SecurityUtils.currentUser().getId(), request);
	}

	@PutMapping("/{id}")
	public ClassDtos.ClassResponse update(@PathVariable Long id, @Valid @RequestBody ClassDtos.UpdateClassRequest request) {
		requireInstructor();
		return classService.update(SecurityUtils.currentUser().getId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	public void delete(@PathVariable Long id) {
		requireInstructor();
		classService.delete(SecurityUtils.currentUser().getId(), id);
	}

	@PostMapping("/{id}/enroll")
	public ClassDtos.ClassResponse enroll(@PathVariable Long id, @Valid @RequestBody ClassDtos.EnrollRequest request) {
		requireInstructor();
		return classService.enroll(SecurityUtils.currentUser().getId(), id, request);
	}

	@PostMapping("/join")
	public ClassDtos.ClassResponse join(@Valid @RequestBody ClassDtos.JoinRequest request) {
		if (SecurityUtils.currentUser().getRole() != Role.STUDENT) {
			throw new com.testpoint.common.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Only students can join with a code");
		}
		return classService.join(SecurityUtils.currentUser().getId(), request);
	}

	private void requireInstructor() {
		if (SecurityUtils.currentUser().getRole() != Role.INSTRUCTOR) {
			throw new com.testpoint.common.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Only instructors can manage classes");
		}
	}
}
