package com.luv2code.springboot.employees.service;

import com.luv2code.springboot.employees.dao.EmployeeRepository;
import com.luv2code.springboot.employees.entity.Employee;
import com.luv2code.springboot.employees.request.EmployeeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService{

    private EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    @Override
    public Employee findById(long id) {
        Optional<Employee> employee = employeeRepository.findById(id);
        Employee theEmployee = null;
        if (employee.isPresent()) {
            theEmployee = employee.get();
        } else {
            // we didn't find the employee
            throw new RuntimeException("Did not find employee id - " + id);
        }
        return theEmployee;
    }


    @Override
    public Employee update(long id, EmployeeRequest employeeRequest) {
        return employeeRepository.save(convertToEmployee(id, employeeRequest));
    }

    @Override
    public Employee save(EmployeeRequest employeeRequest) {
        return employeeRepository.save(convertToEmployee(0, employeeRequest));
    }
    @Override
    public Employee convertToEmployee(long id, EmployeeRequest employeeRequest) {
        return new Employee(id, employeeRequest.getFirstName(), employeeRequest.getLastName(), employeeRequest.getEmail());
    }


    @Override
    public void deleteById(long id) {
        employeeRepository.deleteById(id);

    }


}
