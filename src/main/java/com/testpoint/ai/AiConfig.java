package com.testpoint.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testpoint.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
	@Bean
	AiClient aiClient(AppProperties properties, ObjectMapper objectMapper) {
		AppProperties.Ai ai = properties.ai();
		if (ai.enabled() && ai.apiKey() != null && !ai.apiKey().isBlank()) {
			return new OpenAiCompatibleClient(ai, objectMapper);
		}
		return new DisabledAiClient();
	}
}
