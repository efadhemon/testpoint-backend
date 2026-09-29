package com.testpoint.config;

import com.testpoint.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AppProperties.class)
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final AppProperties appProperties;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AppProperties appProperties) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.appProperties = appProperties;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/grading/**").hasRole("INSTRUCTOR")
						.requestMatchers("/api/analytics/quizzes/**").hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.POST, "/api/questions/**").hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.PUT, "/api/questions/**").hasRole("INSTRUCTOR")
						.requestMatchers(HttpMethod.DELETE, "/api/questions/**").hasRole("INSTRUCTOR")
						.requestMatchers("/api/quizzes/**").hasRole("INSTRUCTOR")
						.requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
						.requestMatchers("/api/student/**").hasRole("STUDENT")
						.anyRequest().authenticated())
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> write(response, 401, "Authentication required"))
						.accessDeniedHandler((request, response, exception) -> write(response, 403, "You do not have access to this resource")))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException(username);
		};
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.stream(appProperties.cors().origins().split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private void write(HttpServletResponse response, int status, String message) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.getWriter().write("{\"message\":\"" + message + "\",\"field\":null,\"errors\":[]}");
	}
}
