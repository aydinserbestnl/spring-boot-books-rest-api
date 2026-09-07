package com.luv2code.springboot.employees.dao;

import com.luv2code.springboot.employees.entity.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserDAOJdbcImpl implements UserDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AppUser> appUserRowMapper = (rs, rowNum) -> new AppUser(
            rs.getString("username"),
            rs.getString("password"),
            rs.getBoolean("active")
    );

    public UserDAOJdbcImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        List<AppUser> results = jdbcTemplate.query(
                "select username, password, active from app_users where username = ?",
                appUserRowMapper, username);
        return results.stream().findFirst();
    }

    @Override
    public List<String> findRolesByUsername(String username) {
        return jdbcTemplate.query(
                "select role from user_roles where username = ?",
                (rs, rowNum) -> rs.getString("role"), username);
    }

    @Override
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from app_users where username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    @Override
    public void createUser(String username, String encodedPassword, boolean active) {
        jdbcTemplate.update(
                "insert into app_users (username, password, active) values (?, ?, ?)",
                username, encodedPassword, active);
    }

    @Override
    public void addRole(String username, String role) {
        jdbcTemplate.update(
                "insert into user_roles (username, role) values (?, ?)",
                username, role);
    }
}
