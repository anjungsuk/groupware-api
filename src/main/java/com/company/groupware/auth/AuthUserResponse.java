package com.company.groupware.auth;

import com.company.groupware.employee.Employee;
import com.company.groupware.employee.EmployeeProfile;

/** 프론트 `AuthUser` 와 1:1 대응 (groupware-front · src/types/auth.ts) */
public record AuthUserResponse(
        Long id,
        String employeeNo,
        String name,
        String email,
        String role,
        String status,
        Long deptId,
        String deptName,
        String positionCode,
        String positionName
) {

    public static AuthUserResponse from(EmployeeProfile profile) {
        Employee e = profile.employee();
        return new AuthUserResponse(
                e.getId(),
                e.getEmployeeNo(),
                e.getName(),
                e.getEmail(),
                e.getRole().name(),
                e.getStatus().name(),
                e.getDeptId(),
                profile.deptName(),
                e.getPositionCode(),
                profile.positionName());
    }
}
