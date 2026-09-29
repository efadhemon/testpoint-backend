package com.testpoint.security;

import com.testpoint.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.testpoint.user.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
	private final SecretKey key;
	private final long expirationMinutes;

	public JwtService(AppProperties properties) {
		String secret = properties.jwt().secret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMinutes = properties.jwt().expirationMinutes();
	}

	public String generate(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("uid", user.getId())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
				.signWith(key)
				.compact();
	}

	public Claims parse(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
