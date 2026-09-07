package com.luv2code.springboot.employees.controller;

import com.luv2code.springboot.employees.service.EmployeeService;
import com.luv2code.springboot.employees.entity.Employee;
import com.luv2code.springboot.employees.request.EmployeeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employee Controller", description = "Controller for managing employees")
public class EmployeeController {
    private EmployeeService employeeService;
    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }
    @Operation(summary = "Get all employees", description = "Retrieve a list of all employees")
    @GetMapping
    public List<Employee> findAll() {
        return employeeService.findAll();
    }
    @Operation(summary = "Get employee by ID", description = "Retrieve an employee by their unique ID")
    @GetMapping("/{id}")
    public Employee findById(@PathVariable long id) {
        return employeeService.findById(id);
    }
    @Operation(summary = "Add a new employee", description = "Create a new employee record")
    @PostMapping
    public Employee addEmployee(@Valid @RequestBody EmployeeRequest employeeRequest) {
        return employeeService.save(employeeRequest);
    }
    @Operation(summary = "Update an existing employee", description = "Update an existing employee record")
    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable long id, @Valid @RequestBody EmployeeRequest employeeRequest) {
        return employeeService.update(id, employeeRequest);
    }
    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable long id) {
        employeeService.deleteById(id);
    }
}
