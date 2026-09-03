package com.company.groupware.employee;

import java.time.LocalDate;

/** 회원가입 입력. 사번은 서버가 채번하므로 포함하지 않는다. */
public record SignupCommand(
        String name,
        String email,
        LocalDate birthDate,
        String zipCode,
        String address,
        String addressDetail,
        String mobilePhone,
        String homePhone,
        EmergencyRelation emergencyRelation,
        String emergencyPhone
) {
}
