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

    /** 결재자 탐색용 — 특정 부서에서 해당 직급인 재직자. 동일 직급이 여럿이면 사번 순 첫 명. */
    Optional<Employee> findFirstByDeptIdAndPositionCodeAndStatusAndDeletedFalseOrderByEmployeeNoAsc(
            Long deptId, String positionCode, EmployeeStatus status);
}
