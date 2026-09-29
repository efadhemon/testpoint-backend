package com.testpoint.common;

import java.util.List;

public record ErrorBody(String message, String field, List<FieldError> errors) {
	public record FieldError(String field, String message) {
	}

	public static ErrorBody of(String message) {
		return new ErrorBody(message, null, List.of());
	}

	public static ErrorBody of(String field, String message) {
		return new ErrorBody(message, field, List.of(new FieldError(field, message)));
	}
}
