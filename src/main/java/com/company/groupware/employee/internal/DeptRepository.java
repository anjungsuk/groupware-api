package com.company.groupware.employee.internal;

import com.company.groupware.employee.Dept;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeptRepository extends JpaRepository<Dept, Long> {
}
