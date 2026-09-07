package com.luv2code.springboot.employees.service;

import com.luv2code.springboot.employees.dao.EmployeeDAO;
import com.luv2code.springboot.employees.entity.Employee;
import com.luv2code.springboot.employees.request.EmployeeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeDAO employeeDAO;

    @Autowired
    public EmployeeServiceImpl(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    @Override
    public List<Employee> findAll() {
        return employeeDAO.findAll();
    }

    @Override
    public Employee findById(long id) {
        return employeeDAO.findById(id);
    }

    @Override
    public Employee update(long id, EmployeeRequest employeeRequest) {
        return employeeDAO.save(convertToEmployee(id, employeeRequest));
    }

    @Override
    public Employee save(EmployeeRequest employeeRequest) {
        return employeeDAO.save(convertToEmployee(0, employeeRequest));
    }

    @Override
    public Employee convertToEmployee(long id, EmployeeRequest employeeRequest) {
        return new Employee(id, employeeRequest.firstName(), employeeRequest.lastName(), employeeRequest.email());
    }

    @Override
    public void deleteById(long id) {
        employeeDAO.deleteById(id);
    }
}
