package com.luv2code.springboot.employees.controller;

import com.luv2code.springboot.employees.entity.UserResponse;
import com.luv2code.springboot.employees.request.UserRegistrationRequest;
import com.luv2code.springboot.employees.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "User Registration", description = "Yeni kullanici ve rol olusturma (H2 console'a gerek yok)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register a new user",
            description = "Duz metin sifreyi alir, BCrypt ile hash'ler ve verilen rollerle birlikte kaydeder. " +
                    "roles ornegi: [\"EMPLOYEE\"], [\"EMPLOYEE\",\"MANAGER\"], [\"EMPLOYEE\",\"MANAGER\",\"ADMIN\"]")
    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody UserRegistrationRequest request) {
        return userService.register(request);
    }
}
