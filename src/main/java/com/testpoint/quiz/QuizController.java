package com.testpoint.quiz;

import com.testpoint.common.PageResponse;
import com.testpoint.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
	private final QuizService quizService;

	public QuizController(QuizService quizService) {
		this.quizService = quizService;
	}

	@GetMapping
	public PageResponse<QuizDtos.QuizResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		return quizService.list(SecurityUtils.currentUser().getId(), page, size);
	}

	@PostMapping
	@ResponseStatus(CREATED)
	public QuizDtos.QuizResponse create(@Valid @RequestBody QuizDtos.QuizRequest request) {
		return quizService.create(SecurityUtils.currentUser().getId(), request);
	}

	@GetMapping("/{id}")
	public QuizDtos.QuizResponse get(@PathVariable Long id) {
		return quizService.get(SecurityUtils.currentUser().getId(), id);
	}

	@PutMapping("/{id}")
	public QuizDtos.QuizResponse update(@PathVariable Long id, @Valid @RequestBody QuizDtos.QuizRequest request) {
		return quizService.update(SecurityUtils.currentUser().getId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	public void delete(@PathVariable Long id) {
		quizService.delete(SecurityUtils.currentUser().getId(), id);
	}

	@PostMapping("/{id}/questions")
	public QuizDtos.QuizResponse addQuestion(@PathVariable Long id, @Valid @RequestBody QuizDtos.AddQuestionRequest request) {
		return quizService.addQuestion(SecurityUtils.currentUser().getId(), id, request);
	}

	@DeleteMapping("/{id}/questions/{questionId}")
	public QuizDtos.QuizResponse removeQuestion(@PathVariable Long id, @PathVariable Long questionId) {
		return quizService.removeQuestion(SecurityUtils.currentUser().getId(), id, questionId);
	}

	@PostMapping("/{id}/publish")
	public QuizDtos.QuizResponse publish(@PathVariable Long id) {
		return quizService.publish(SecurityUtils.currentUser().getId(), id);
	}

	@PostMapping("/{id}/close")
	public QuizDtos.QuizResponse close(@PathVariable Long id) {
		return quizService.close(SecurityUtils.currentUser().getId(), id);
	}

	@PostMapping("/{id}/assign")
	public QuizDtos.QuizResponse assign(@PathVariable Long id, @Valid @RequestBody QuizDtos.AssignRequest request) {
		return quizService.assign(SecurityUtils.currentUser().getId(), id, request);
	}
}
