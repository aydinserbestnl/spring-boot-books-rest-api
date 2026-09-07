package com.luv2code.springboot.employees.entity;

import java.util.List;

public record UserResponse(String username, List<String> roles) {
}
