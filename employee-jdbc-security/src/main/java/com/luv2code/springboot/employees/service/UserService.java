package com.luv2code.springboot.employees.service;

import com.luv2code.springboot.employees.entity.UserResponse;
import com.luv2code.springboot.employees.request.UserRegistrationRequest;

public interface UserService {
    UserResponse register(UserRegistrationRequest request);
}
