package com.company.groupware.employee;

/** 계정 상태. 가입 직후 PENDING 이며 관리자 승인(ACTIVE) 전에는 로그인할 수 없다. */
public enum EmployeeStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    RESIGNED
}
