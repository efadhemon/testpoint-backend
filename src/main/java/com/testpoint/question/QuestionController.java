package com.testpoint.question;

import com.testpoint.common.PageResponse;
import com.testpoint.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
	private final QuestionService questionService;

	public QuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@GetMapping
	public PageResponse<QuestionDtos.QuestionResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		return questionService.list(SecurityUtils.currentUser().getId(), page, size);
	}

	@PostMapping
	@ResponseStatus(CREATED)
	public QuestionDtos.QuestionResponse create(@Valid @RequestBody QuestionDtos.QuestionRequest request) {
		return questionService.create(SecurityUtils.currentUser().getId(), request);
	}

	@PostMapping("/bulk")
	@ResponseStatus(CREATED)
	public List<QuestionDtos.QuestionResponse> createAll(@Valid @RequestBody QuestionDtos.BulkQuestionRequest request) {
		return questionService.createAll(SecurityUtils.currentUser().getId(), request.questions());
	}

	@PostMapping(value = "/generate-from-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public List<QuestionDtos.QuestionRequest> generate(@RequestPart("file") MultipartFile file) {
		return questionService.generateFromPdf(file);
	}

	@PutMapping("/{id}")
	public QuestionDtos.QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionDtos.QuestionRequest request) {
		return questionService.update(SecurityUtils.currentUser().getId(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(NO_CONTENT)
	public void delete(@PathVariable Long id) {
		questionService.delete(SecurityUtils.currentUser().getId(), id);
	}
}
