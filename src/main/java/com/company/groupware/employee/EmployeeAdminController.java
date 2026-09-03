package com.company.groupware.employee;

import com.company.groupware.common.response.ApiResponse;
import com.company.groupware.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 사원 관리 API — docs/04_인증_API_명세.md §5.1.
 * 가입은 PENDING 으로 생성되므로 여기서 승인해야 로그인할 수 있다.
 */
@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class EmployeeAdminController {

    private final EmployeeService employeeService;

    /** 상태별 사원 목록. 기본값은 승인 대기 큐다. */
    @GetMapping
    public ApiResponse<PageResponse<EmployeeSummaryResponse>> list(
            @RequestParam(defaultValue = "PENDING") EmployeeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(employeeService.findByStatus(status, pageable));
    }

    /** 승인 — 부서·직급·입사일을 배정하고 ACTIVE 로 전환한다. */
    @PostMapping("/{id}/approve")
    public ApiResponse<EmployeeSummaryResponse> approve(@PathVariable Long id,
                                                       @Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(employeeService.approve(id, request));
    }

    /** 거절 — 로그인 시 A006 으로 막힌다. */
    @PostMapping("/{id}/reject")
    public ApiResponse<EmployeeSummaryResponse> reject(@PathVariable Long id) {
        return ApiResponse.ok(employeeService.reject(id));
    }
}
