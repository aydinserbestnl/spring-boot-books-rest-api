package com.luv2code.springboot.employees.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserRegistrationRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 100, message = "Password must be at least 4 characters")
        String password,

        @NotEmpty(message = "At least one role is required, e.g. EMPLOYEE, MANAGER, ADMIN")
        List<String> roles
) {
}
