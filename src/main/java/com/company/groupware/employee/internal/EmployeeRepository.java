package com.company.groupware.employee.internal;

import com.company.groupware.employee.Employee;
import com.company.groupware.employee.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    Page<Employee> findByStatusAndDeletedFalse(EmployeeStatus status, Pageable pageable);
}
