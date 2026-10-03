package com.testpoint.user;

import com.testpoint.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/register")
	@ResponseStatus(CREATED)
	public UserDtos.AuthResponse register(@Valid @RequestBody UserDtos.RegisterRequest request) {
		return userService.register(request);
	}

	@PostMapping("/login")
	public UserDtos.AuthResponse login(@Valid @RequestBody UserDtos.LoginRequest request) {
		return userService.login(request);
	}

	@GetMapping("/me")
	public UserDtos.UserResponse me() {
		return userService.me(SecurityUtils.currentUser().getId());
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(name = "name", defaultValue = "World") String name) {
		System.out.println(name);
		return "Hello, " + name;
	}
}
