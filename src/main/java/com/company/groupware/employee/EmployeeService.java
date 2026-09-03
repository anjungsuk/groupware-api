package com.company.groupware.employee;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import com.company.groupware.common.exception.FieldValidationException;
import com.company.groupware.employee.internal.DeptRepository;
import com.company.groupware.employee.internal.EmployeeNoGenerator;
import com.company.groupware.employee.internal.EmployeeRepository;
import com.company.groupware.employee.internal.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/** 사원 도메인의 공식 API. 다른 모듈은 이 서비스를 통해서만 접근한다. */
@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DeptRepository deptRepository;
    private final PositionRepository positionRepository;
    private final EmployeeNoGenerator employeeNoGenerator;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DeptRepository deptRepository,
                           PositionRepository positionRepository,
                           EmployeeNoGenerator employeeNoGenerator) {
        this.employeeRepository = employeeRepository;
        this.deptRepository = deptRepository;
        this.positionRepository = positionRepository;
        this.employeeNoGenerator = employeeNoGenerator;
    }

    /**
     * 회원가입 — 승인 대기(PENDING) 상태로 생성한다.
     * 사번은 여기서 채번한다. 클라이언트가 보내는 값이 아니다.
     */
    @Transactional
    public Employee signup(SignupCommand command, String encodedPassword) {
        if (employeeRepository.existsByEmailAndDeletedFalse(command.email())) {
            throw FieldValidationException.of("email", "이미 사용중인 이메일입니다.");
        }

        Employee employee = Employee.pendingSignup(
                employeeNoGenerator.next(),
                command.name(),
                command.email(),
                encodedPassword,
                command.birthDate(),
                command.zipCode(),
                command.address(),
                command.addressDetail(),
                command.mobilePhone(),
                command.homePhone(),
                command.emergencyRelation(),
                command.emergencyPhone());

        return employeeRepository.save(employee);
    }

    public Optional<Employee> findActiveByEmail(String email) {
        return employeeRepository.findByEmailAndDeletedFalse(email);
    }

    /** 관리자 승인 — 부서·직급을 배정하고 로그인 가능 상태로 만든다. */
    @Transactional
    public Employee approve(Long employeeId, Long deptId, String positionCode, LocalDate hireDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!deptRepository.existsById(deptId)) {
            throw FieldValidationException.of("deptId", "존재하지 않는 부서입니다.");
        }
        if (!positionRepository.existsById(positionCode)) {
            throw FieldValidationException.of("positionCode", "존재하지 않는 직급입니다.");
        }

        employee.approve(deptId, positionCode, hireDate);
        return employee;
    }

    /** 화면 표시용 부서·직급 이름. 승인 전이면 둘 다 null 이다. */
    public EmployeeProfile toProfile(Employee employee) {
        String deptName = employee.getDeptId() == null ? null
                : deptRepository.findById(employee.getDeptId()).map(Dept::getName).orElse(null);
        String positionName = employee.getPositionCode() == null ? null
                : positionRepository.findById(employee.getPositionCode())
                        .map(Position::getName).orElse(null);

        return new EmployeeProfile(employee, deptName, positionName);
    }
}
