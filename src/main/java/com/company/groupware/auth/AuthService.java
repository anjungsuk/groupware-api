package com.company.groupware.auth;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import com.company.groupware.common.security.jwt.JwtProvider;
import com.company.groupware.employee.Employee;
import com.company.groupware.employee.EmployeeProfile;
import com.company.groupware.employee.EmployeeService;
import com.company.groupware.employee.EmployeeStatus;
import com.company.groupware.employee.SignupCommand;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(EmployeeService employeeService,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.employeeService = employeeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public SignupResponse signup(SignupRequest request) {
        SignupCommand command = new SignupCommand(
                request.name(),
                request.email(),
                request.birthDate(),
                request.zipCode(),
                request.address(),
                request.addressDetail(),
                request.mobilePhone(),
                request.normalizedHomePhone(),
                request.emergencyRelation(),
                request.emergencyPhone());

        Employee saved = employeeService.signup(command, passwordEncoder.encode(request.password()));
        return SignupResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeService.findActiveByEmail(request.email())
                // 계정 존재 여부가 드러나지 않도록 메시지를 통일한다
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.LOGIN_FAILED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), employee.getPassword())) {
            throw new BusinessException(
                    ErrorCode.LOGIN_FAILED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 비밀번호가 맞아도 승인 전이면 로그인시키지 않는다
        if (!employee.canLogin()) {
            throw new BusinessException(employee.getStatus() == EmployeeStatus.REJECTED
                    ? ErrorCode.ACCOUNT_REJECTED
                    : ErrorCode.ACCOUNT_NOT_APPROVED);
        }

        EmployeeProfile profile = employeeService.toProfile(employee);
        return new LoginResponse(
                jwtProvider.createAccessToken(String.valueOf(employee.getId()), employee.getRole().name()),
                jwtProvider.createRefreshToken(String.valueOf(employee.getId())),
                AuthUserResponse.from(profile));
    }
}
