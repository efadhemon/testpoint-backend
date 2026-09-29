package com.testpoint.security;

import com.testpoint.common.ApiException;
import com.testpoint.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
	private SecurityUtils() {
	}

	public static User currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return user;
	}
}
