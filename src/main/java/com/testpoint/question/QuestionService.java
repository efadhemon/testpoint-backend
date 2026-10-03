package com.testpoint.question;

import com.testpoint.ai.AiClient;
import com.testpoint.common.ApiException;
import com.testpoint.common.PageResponse;
import com.testpoint.quiz.QuizQuestionRepository;
import com.testpoint.user.UserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionService {
	private final QuestionRepository questionRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final UserRepository userRepository;
	private final AiClient aiClient;

	public QuestionService(QuestionRepository questionRepository, QuizQuestionRepository quizQuestionRepository, UserRepository userRepository, AiClient aiClient) {
		this.questionRepository = questionRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.userRepository = userRepository;
		this.aiClient = aiClient;
	}

	@Transactional(readOnly = true)
	public PageResponse<QuestionDtos.QuestionResponse> list(Long instructorId, int page, int size) {
		if (page < 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "page", "Page must be 0 or greater");
		}
		if (size < 1 || size > 100) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "size", "Size must be between 1 and 100");
		}
		Page<Question> result = questionRepository.findByInstructorId(
				instructorId,
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
		return new PageResponse<>(
				result.getContent().stream().map(QuestionDtos.QuestionResponse::from).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	@Transactional
	public QuestionDtos.QuestionResponse create(Long instructorId, QuestionDtos.QuestionRequest request) {
		Question question = new Question();
		question.setInstructor(userRepository.getReferenceById(instructorId));
		apply(question, request);
		return QuestionDtos.QuestionResponse.from(questionRepository.save(question));
	}

	@Transactional
	public List<QuestionDtos.QuestionResponse> createAll(Long instructorId, List<QuestionDtos.QuestionRequest> requests) {
		List<QuestionDtos.QuestionResponse> saved = new ArrayList<>();
		for (QuestionDtos.QuestionRequest request : requests) {
			saved.add(create(instructorId, request));
		}
		return saved;
	}

	@Transactional
	public QuestionDtos.QuestionResponse update(Long instructorId, Long questionId, QuestionDtos.QuestionRequest request) {
		Question question = owned(instructorId, questionId);
		apply(question, request);
		return QuestionDtos.QuestionResponse.from(question);
	}

	@Transactional
	public void delete(Long instructorId, Long questionId) {
		owned(instructorId, questionId);
		if (quizQuestionRepository.existsByQuestion_Id(questionId)) {
			throw new ApiException(HttpStatus.CONFLICT, "Remove this question from its quizzes before deleting it");
		}
		questionRepository.deleteById(questionId);
	}

	public List<QuestionDtos.QuestionRequest> generateFromPdf(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "file", "Choose a PDF to upload");
		}
		String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
		if (!name.endsWith(".pdf")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "file", "Upload a PDF file");
		}
		String text = extractText(file);
		if (text.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "file", "No readable text was found in that PDF");
		}
		return aiClient.generateQuestions(text);
	}

	public Question owned(Long instructorId, Long questionId) {
		Question question = questionRepository.findById(questionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
		if (!question.getInstructor().getId().equals(instructorId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this question");
		}
		return question;
	}

	private void apply(Question question, QuestionDtos.QuestionRequest request) {
		question.setType(request.type());
		question.setText(request.text().trim());
		question.setMarks(request.marks());
		question.setExplanation(blankToNull(request.explanation()));
		question.setModelAnswer(null);
		question.setCorrectBoolean(null);
		question.getOptions().clear();
		switch (request.type()) {
			case MCQ -> applyMcq(question, request.options());
			case TRUE_FALSE -> {
				if (request.correctBoolean() == null) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "correctBoolean", "Choose true or false");
				}
				question.setCorrectBoolean(request.correctBoolean());
			}
			case SHORT_ANSWER -> {
				if (request.modelAnswer() == null || request.modelAnswer().isBlank()) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "modelAnswer", "Add a model answer or rubric");
				}
				question.setModelAnswer(request.modelAnswer().trim());
			}
		}
	}

	private void applyMcq(Question question, List<QuestionDtos.OptionRequest> options) {
		if (options == null || options.size() < 2 || options.size() > 8) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "options", "Add between 2 and 8 options");
		}
		long correct = options.stream().filter(QuestionDtos.OptionRequest::correct).count();
		if (correct != 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "options", "Mark exactly one correct option");
		}
		int position = 1;
		for (QuestionDtos.OptionRequest option : options) {
			QuestionOption entity = new QuestionOption();
			entity.setQuestion(question);
			entity.setText(option.text().trim());
			entity.setCorrect(option.correct());
			entity.setPosition(position++);
			question.getOptions().add(entity);
		}
	}

	private String extractText(MultipartFile file) {
		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
			if (document.getNumberOfPages() == 0) {
				return "";
			}
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setEndPage(Math.min(document.getNumberOfPages(), 20));
			String text = stripper.getText(document).trim();
			return text.length() > 15000 ? text.substring(0, 15000) : text;
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "file", "That PDF could not be read");
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
