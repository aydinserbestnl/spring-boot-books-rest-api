package com.luv2code.springboot.employees.dao;

import com.luv2code.springboot.employees.entity.AppUser;

import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Optional<AppUser> findByUsername(String username);
    List<String> findRolesByUsername(String username);
    boolean existsByUsername(String username);
    void createUser(String username, String encodedPassword, boolean active);
    void addRole(String username, String role);
}
