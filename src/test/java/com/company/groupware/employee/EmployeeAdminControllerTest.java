package com.company.groupware.employee;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import com.company.groupware.common.exception.FieldValidationException;
import com.company.groupware.common.exception.GlobalExceptionHandler;
import com.company.groupware.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 승인 API 계약 — docs/04_인증_API_명세.md §5.1.
 *
 * standaloneSetup 이라 @PreAuthorize 는 적용되지 않는다.
 * 권한 차단(ROLE_MEMBER → 403 C006)은 SecurityConfig + 메서드 시큐리티의 몫이다.
 */
class EmployeeAdminControllerTest {

    private MockMvc mockMvc;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EmployeeAdminController(employeeService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static EmployeeSummaryResponse pending() {
        return new EmployeeSummaryResponse(7L, "EMP-2026-0001", "김철수", "kim@company.co.kr",
                "010-1234-5678", "PENDING", null, null, null, null, null, Instant.EPOCH);
    }

    private static EmployeeSummaryResponse approved() {
        return new EmployeeSummaryResponse(7L, "EMP-2026-0001", "김철수", "kim@company.co.kr",
                "010-1234-5678", "ACTIVE", 2L, "물류운영팀", "STAFF", "사원",
                LocalDate.of(2026, 9, 1), Instant.EPOCH);
    }

    private static final String VALID_APPROVE = """
            {"deptId": 2, "positionCode": "STAFF", "hireDate": "2026-09-01"}
            """;

    @Test
    @DisplayName("기본 목록은 승인 대기(PENDING) 큐이고 민감 정보를 담지 않는다")
    void listDefaultsToPending() throws Exception {
        given(employeeService.findByStatus(eq(EmployeeStatus.PENDING), any()))
                .willReturn(new PageResponse<>(List.of(pending()), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/admin/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].employeeNo").value("EMP-2026-0001"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].deptName").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].address").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("승인하면 부서·직급이 배정된 ACTIVE 사원이 돌아온다")
    void approveAssignsDeptAndPosition() throws Exception {
        given(employeeService.approve(eq(7L), any())).willReturn(approved());

        mockMvc.perform(post("/api/v1/admin/employees/7/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_APPROVE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.deptName").value("물류운영팀"))
                .andExpect(jsonPath("$.data.positionName").value("사원"))
                .andExpect(jsonPath("$.data.hireDate").value("2026-09-01"));
    }

    @Test
    @DisplayName("부서·직급·입사일이 빠지면 C001 과 필드별 메시지로 응답한다")
    void approveRequiresAssignment() throws Exception {
        mockMvc.perform(post("/api/v1/admin/employees/7/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.deptId").value("부서를 선택해 주세요."))
                .andExpect(jsonPath("$.data.positionCode").value("직급을 선택해 주세요."))
                .andExpect(jsonPath("$.data.hireDate").value("입사일을 입력해 주세요."));
    }

    @Test
    @DisplayName("없는 부서로 승인하면 C001 + data.deptId 로 응답한다")
    void approveRejectsUnknownDept() throws Exception {
        willThrow(FieldValidationException.of("deptId", "존재하지 않는 부서입니다."))
                .given(employeeService).approve(eq(7L), any());

        mockMvc.perform(post("/api/v1/admin/employees/7/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_APPROVE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.data.deptId").value("존재하지 않는 부서입니다."));
    }

    @Test
    @DisplayName("없는 사원을 승인하면 U001 로 응답한다")
    void approveRejectsUnknownEmployee() throws Exception {
        willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND))
                .given(employeeService).approve(eq(99L), any());

        mockMvc.perform(post("/api/v1/admin/employees/99/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_APPROVE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }

    @Test
    @DisplayName("거절하면 REJECTED 로 바뀐다")
    void rejectMarksRejected() throws Exception {
        given(employeeService.reject(7L)).willReturn(new EmployeeSummaryResponse(
                7L, "EMP-2026-0001", "김철수", "kim@company.co.kr", "010-1234-5678",
                "REJECTED", null, null, null, null, null, Instant.EPOCH));

        mockMvc.perform(post("/api/v1/admin/employees/7/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
