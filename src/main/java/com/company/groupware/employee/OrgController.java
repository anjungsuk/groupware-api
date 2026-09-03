package com.company.groupware.employee;

import com.company.groupware.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 조직 마스터 조회 — 관리자 승인 화면의 부서·직급 선택지.
 * 로그인만 하면 볼 수 있다. 개인정보가 아니라 조직도이므로 관리자로 제한하지 않는다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrgController {

    private final EmployeeService employeeService;

    @GetMapping("/depts")
    public ApiResponse<List<DeptResponse>> depts() {
        return ApiResponse.ok(employeeService.findDepts());
    }

    @GetMapping("/positions")
    public ApiResponse<List<PositionResponse>> positions() {
        return ApiResponse.ok(employeeService.findPositions());
    }
}
