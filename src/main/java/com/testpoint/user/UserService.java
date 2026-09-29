package com.testpoint.user;

import com.testpoint.common.ApiException;
import com.testpoint.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public UserDtos.AuthResponse register(UserDtos.RegisterRequest request) {
		if (request.role() == Role.ADMIN) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "role", "Admin accounts are created by the system");
		}
		String email = request.email().trim().toLowerCase();
		if (userRepository.findByEmail(email).isPresent()) {
			throw new ApiException(HttpStatus.CONFLICT, "email", "An account with this email already exists");
		}
		User user = new User();
		user.setName(request.name().trim());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(request.role());
		user.setEnabled(true);
		userRepository.save(user);
		return new UserDtos.AuthResponse(jwtService.generate(user), UserDtos.UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public UserDtos.AuthResponse login(UserDtos.LoginRequest request) {
		User user = userRepository.findByEmail(request.email().trim().toLowerCase())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"));
		if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
		}
		return new UserDtos.AuthResponse(jwtService.generate(user), UserDtos.UserResponse.from(user));
	}

	@Transactional(readOnly = true)
	public UserDtos.UserResponse me(Long userId) {
		return UserDtos.UserResponse.from(require(userId));
	}

	@Transactional(readOnly = true)
	public List<UserDtos.UserResponse> listUsers() {
		return userRepository.findAll().stream().map(UserDtos.UserResponse::from).toList();
	}

	@Transactional
	public UserDtos.UserResponse updateUser(Long actorId, Long userId, UserDtos.UpdateUserRequest request) {
		if (actorId.equals(userId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change your own account from this screen");
		}
		User user = require(userId);
		if (request.role() != null) {
			if (user.getRole() == Role.ADMIN && request.role() != Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "role", "At least one admin must remain");
			}
			user.setRole(request.role());
		}
		if (request.enabled() != null) {
			if (user.getRole() == Role.ADMIN && !request.enabled() && userRepository.countByRole(Role.ADMIN) <= 1) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "enabled", "At least one admin must remain");
			}
			user.setEnabled(request.enabled());
		}
		return UserDtos.UserResponse.from(user);
	}

	public User require(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
	}
}
