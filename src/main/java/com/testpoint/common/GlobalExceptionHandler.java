package com.testpoint.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorBody> handleApi(ApiException exception) {
		return ResponseEntity.status(exception.getStatus())
				.body(ErrorBody.of(exception.getField(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException exception) {
		List<ErrorBody.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorBody.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		String field = errors.isEmpty() ? null : errors.get(0).field();
		String message = errors.isEmpty() ? "Validation failed" : errors.get(0).message();
		return ResponseEntity.badRequest().body(new ErrorBody(message, field, errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorBody> handleUnreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(ErrorBody.of("Request body is not valid JSON"));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorBody> handleDenied(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorBody.of("You do not have access to this resource"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorBody> handleOther(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorBody.of("Something went wrong"));
	}
}
