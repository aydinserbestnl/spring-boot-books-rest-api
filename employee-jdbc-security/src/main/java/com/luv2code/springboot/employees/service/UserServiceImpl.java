package com.luv2code.springboot.employees.service;

import com.luv2code.springboot.employees.dao.UserDAO;
import com.luv2code.springboot.employees.entity.UserResponse;
import com.luv2code.springboot.employees.request.UserRegistrationRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse register(UserRegistrationRequest request) {
        if (userDAO.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists: " + request.username());
        }

        // duz metin sifre burada, kaydetmeden once BCrypt ile hash'leniyor
        String encodedPassword = passwordEncoder.encode(request.password());
        userDAO.createUser(request.username(), encodedPassword, true);

        for (String role : request.roles()) {
            userDAO.addRole(request.username(), "ROLE_" + role.toUpperCase());
        }

        return new UserResponse(request.username(), request.roles());
    }
}
