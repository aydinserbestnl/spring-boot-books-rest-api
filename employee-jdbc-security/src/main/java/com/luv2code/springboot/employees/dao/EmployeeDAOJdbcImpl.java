package com.luv2code.springboot.employees.dao;

import com.luv2code.springboot.employees.entity.Employee;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class EmployeeDAOJdbcImpl implements EmployeeDAO {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Employee> employeeRowMapper = (rs, rowNum) -> new Employee(
            rs.getLong("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email")
    );

    public EmployeeDAOJdbcImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Employee> findAll() {
        return jdbcTemplate.query("select id, first_name, last_name, email from employee", employeeRowMapper);
    }

    @Override
    public Employee findById(long id) {
        List<Employee> results = jdbcTemplate.query(
                "select id, first_name, last_name, email from employee where id = ?",
                employeeRowMapper, id);
        if (results.isEmpty()) {
            throw new RuntimeException("Did not find employee id - " + id);
        }
        return results.get(0);
    }

    @Override
    public Employee save(Employee employee) {
        if (employee.id() == 0) {
            return insert(employee);
        }
        return update(employee);
    }

    private Employee insert(Employee employee) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into employee (first_name, last_name, email) values (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, employee.firstName());
            ps.setString(2, employee.lastName());
            ps.setString(3, employee.email());
            return ps;
        }, keyHolder);

        long newId = keyHolder.getKey().longValue();
        return new Employee(newId, employee.firstName(), employee.lastName(), employee.email());
    }

    private Employee update(Employee employee) {
        jdbcTemplate.update(
                "update employee set first_name = ?, last_name = ?, email = ? where id = ?",
                employee.firstName(), employee.lastName(), employee.email(), employee.id());
        return employee;
    }

    @Override
    public void deleteById(long id) {
        jdbcTemplate.update("delete from employee where id = ?", id);
    }
}
