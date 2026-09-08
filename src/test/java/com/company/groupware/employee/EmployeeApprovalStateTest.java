package com.company.groupware.employee;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.employee.internal.DeptRepository;
import com.company.groupware.employee.internal.EmployeeNoGenerator;
import com.company.groupware.employee.internal.EmployeeRepository;
import com.company.groupware.employee.internal.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 승인 큐의 상태 머신을 고정한다.
 *
 * PENDING  --approve--> ACTIVE
 * PENDING  --reject-->  REJECTED
 * REJECTED --approve--> ACTIVE   (거절 복구)
 * ACTIVE   --approve--> 거부      (소속 변경은 인사이동이지 승인이 아니다)
 * ACTIVE   --reject-->  거부      (재직자 잠금은 퇴사 처리이지 가입 거절이 아니다)
 */
class EmployeeApprovalStateTest {

    private EmployeeRepository employeeRepository;
    private EmployeeService employeeService;

    private static final ApproveRequest ASSIGNMENT =
            new ApproveRequest(2L, "STAFF", LocalDate.of(2026, 9, 1));

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        DeptRepository deptRepository = mock(DeptRepository.class);
        PositionRepository positionRepository = mock(PositionRepository.class);

        given(deptRepository.existsById(anyLong())).willReturn(true);
        given(positionRepository.existsById(anyString())).willReturn(true);
        given(deptRepository.findById(any())).willReturn(Optional.empty());
        given(positionRepository.findById(any())).willReturn(Optional.empty());

        employeeService = new EmployeeService(
                employeeRepository, deptRepository, positionRepository,
                mock(EmployeeNoGenerator.class));
    }

    /** 승인 대기 상태의 신규 가입자 */
    private Employee pending() {
        Employee employee = Employee.pendingSignup(
                "EMP-2026-0001", "김철수", "kim@company.co.kr", "encoded",
                LocalDate.of(1990, 5, 14), "06236", "서울 강남구 테헤란로 1", "101동 1001호",
                "010-1234-5678", null, EmergencyRelation.SPOUSE, "010-9876-5432");
        given(employeeRepository.findById(1L)).willReturn(Optional.of(employee));
        return employee;
    }

    @Test
    @DisplayName("승인 대기 신청을 승인하면 소속이 배정되고 로그인할 수 있다")
    void approvePendingActivates() {
        Employee employee = pending();

        employeeService.approve(1L, ASSIGNMENT);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.getDeptId()).isEqualTo(2L);
        assertThat(employee.getPositionCode()).isEqualTo("STAFF");
        assertThat(employee.getHireDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(employee.canLogin()).isTrue();
    }

    @Test
    @DisplayName("승인 대기 신청을 거절하면 로그인할 수 없다")
    void rejectPendingBlocksLogin() {
        Employee employee = pending();

        employeeService.reject(1L);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.REJECTED);
        assertThat(employee.canLogin()).isFalse();
    }

    @Test
    @DisplayName("거절된 신청도 다시 승인할 수 있다 — 오클릭 복구 경로")
    void approveRejectedRecovers() {
        Employee employee = pending();
        employeeService.reject(1L);

        employeeService.approve(1L, ASSIGNMENT);

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.canLogin()).isTrue();
    }

    @Test
    @DisplayName("이미 승인된 사원은 다시 승인할 수 없다 — 소속 변경은 인사이동이다")
    void approveActiveIsRejected() {
        Employee employee = pending();
        employeeService.approve(1L, ASSIGNMENT);

        assertThatThrownBy(() -> employeeService.approve(1L, new ApproveRequest(3L, "MANAGER", LocalDate.of(2026, 1, 1))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 승인된 계정입니다.");

        // 소속이 바뀌지 않았다
        assertThat(employee.getDeptId()).isEqualTo(2L);
        assertThat(employee.getPositionCode()).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("소프트 삭제된 사원은 승인으로 되살릴 수 없다")
    void approveDeletedIsRejected() {
        Employee employee = pending();
        employee.softDelete("system");

        assertThatThrownBy(() -> employeeService.approve(1L, ASSIGNMENT))
                .isInstanceOf(BusinessException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.PENDING);
        assertThat(employee.canLogin()).isFalse();
    }

    @Test
    @DisplayName("소프트 삭제된 사원은 거절 대상도 아니다")
    void rejectDeletedIsRejected() {
        Employee employee = pending();
        employee.softDelete("system");

        assertThatThrownBy(() -> employeeService.reject(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("재직 중인 사원은 거절할 수 없다 — 로그인이 막히면 안 된다")
    void rejectActiveIsRejected() {
        Employee employee = pending();
        employeeService.approve(1L, ASSIGNMENT);

        assertThatThrownBy(() -> employeeService.reject(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("승인 대기 중인 신청만 거절할 수 있습니다.");

        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.canLogin()).isTrue();
    }

    @Test
    @DisplayName("입사일이 미래여도 승인은 그대로 받아들인다 — 입사 예정일 등록을 막지 않는다")
    void approveAcceptsFutureHireDate() {
        Employee employee = pending();
        LocalDate future = LocalDate.now().plusMonths(1);

        employeeService.approve(1L, new ApproveRequest(2L, "STAFF", future));

        assertThat(employee.getHireDate()).isEqualTo(future);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }
}
