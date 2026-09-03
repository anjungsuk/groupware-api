package com.company.groupware.auth;

import com.company.groupware.employee.EmergencyRelation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 회원가입 요청 — docs/04_인증_API_명세.md 와 프론트 `SignupRequest` 에 대응.
 * 사번은 서버가 채번하고, 부서·직급은 관리자가 승인 시 배정하므로 받지 않는다.
 * 프론트 검증은 UX 용이다. 여기서 반드시 다시 검증한다.
 */
public record SignupRequest(

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 50, message = "이름은 50자 이내입니다.")
        String name,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100)
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.")
        String password,

        @NotNull(message = "생년월일을 입력해 주세요.")
        @Past(message = "생년월일이 올바르지 않습니다.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate birthDate,

        @NotBlank(message = "우편번호를 입력해 주세요.")
        @Size(max = 10)
        String zipCode,

        @NotBlank(message = "주소를 입력해 주세요.")
        @Size(max = 255)
        String address,

        @NotBlank(message = "상세주소를 입력해 주세요.")
        @Size(max = 100)
        String addressDetail,

        @NotBlank(message = "휴대폰 번호를 입력해 주세요.")
        @Pattern(regexp = "^01[016-9]-?\\d{3,4}-?\\d{4}$",
                message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String mobilePhone,

        // 선택 항목. 값이 있으면 형식을 검사한다.
        @Pattern(regexp = "^$|^0\\d{1,2}-?\\d{3,4}-?\\d{4}$",
                message = "집전화 번호 형식이 올바르지 않습니다.")
        String homePhone,

        @NotNull(message = "비상연락망 관계를 선택해 주세요.")
        EmergencyRelation emergencyRelation,

        @NotBlank(message = "비상연락처를 입력해 주세요.")
        @Pattern(regexp = "^(01[016-9]|0\\d{1,2})-?\\d{3,4}-?\\d{4}$",
                message = "비상연락처 번호 형식이 올바르지 않습니다.")
        String emergencyPhone
) {

    /** 빈 문자열로 온 선택 항목은 null 로 정규화한다. */
    public String normalizedHomePhone() {
        return homePhone == null || homePhone.isBlank() ? null : homePhone;
    }
}
