package com.company.groupware.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 가입 승인 — 관리자가 소속을 배정한다. */
public record ApproveRequest(

        @NotNull(message = "부서를 선택해 주세요.")
        Long deptId,

        @NotBlank(message = "직급을 선택해 주세요.")
        String positionCode,

        @NotNull(message = "입사일을 입력해 주세요.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hireDate
) {
}
