package com.testpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRulesTest {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void registerAndLoginRejectMissingTokenAndStudentAdminAccess() throws Exception {
		String body = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Test Student","email":"student@example.com","password":"password123","role":"STUDENT"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.role").value("STUDENT"))
				.andReturn().getResponse().getContentAsString();
		JsonNode auth = objectMapper.readTree(body);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"student@example.com","password":"password123"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + auth.get("token").asText()))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Bad Admin","email":"admin2@example.com","password":"password123","role":"ADMIN"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.field").value("role"));
	}
}
