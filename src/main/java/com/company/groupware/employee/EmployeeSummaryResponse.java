package com.company.groupware.employee;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 관리자 화면용 사원 요약.
 * 생년월일·주소·비상연락처 같은 민감 정보는 담지 않는다 (docs/04 §5.6).
 */
public record EmployeeSummaryResponse(
        Long id,
        String employeeNo,
        String name,
        String email,
        String mobilePhone,
        String status,
        Long deptId,
        String deptName,
        String positionCode,
        String positionName,
        LocalDate hireDate,
        Instant createdAt
) {

    public static EmployeeSummaryResponse from(EmployeeProfile profile) {
        Employee e = profile.employee();
        return new EmployeeSummaryResponse(
                e.getId(),
                e.getEmployeeNo(),
                e.getName(),
                e.getEmail(),
                e.getMobilePhone(),
                e.getStatus().name(),
                e.getDeptId(),
                profile.deptName(),
                e.getPositionCode(),
                profile.positionName(),
                e.getHireDate(),
                e.getCreatedAt());
    }
}
