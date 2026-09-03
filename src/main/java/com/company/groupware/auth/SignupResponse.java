package com.company.groupware.auth;

import com.company.groupware.employee.Employee;

/** 가입 결과. 토큰을 주지 않는다 — 관리자 승인 전에는 로그인할 수 없다. */
public record SignupResponse(
        String employeeNo,
        String name,
        String email,
        String status
) {

    public static SignupResponse from(Employee e) {
        return new SignupResponse(e.getEmployeeNo(), e.getName(), e.getEmail(), e.getStatus().name());
    }
}
