package com.testpoint.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Ai ai) {
	public record Jwt(String secret, long expirationMinutes) {
	}

	public record Cors(String origins) {
	}

	public record Ai(boolean enabled, String apiKey, String baseUrl, String model) {
	}
}
