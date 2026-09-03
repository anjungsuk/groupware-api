package com.company.groupware.employee;

/** 사원 + 부서·직급 이름. 승인 전이면 deptName·positionName 이 null 이다. */
public record EmployeeProfile(
        Employee employee,
        String deptName,
        String positionName
) {
}
