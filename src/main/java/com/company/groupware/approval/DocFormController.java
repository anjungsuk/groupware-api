package com.company.groupware.approval;

import com.company.groupware.common.response.ApiResponse;
import com.company.groupware.employee.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 양식 조회 — TRD §5 `GET /forms`.
 * 결재 작성 화면이 필드 정의로 폼을 그리고, 결재선 미리보기로 상신 전에 결재자를 확인한다.
 */
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class DocFormController {

    private final ApprovalService approvalService;
    private final EmployeeService employeeService;

    @GetMapping
    public ApiResponse<List<DocFormResponse>> list() {
        return ApiResponse.ok(approvalService.findForms().stream()
                .map(DocFormResponse::from)
                .toList());
    }

    @GetMapping("/{code}")
    public ApiResponse<DocFormResponse> detail(@PathVariable String code) {
        return ApiResponse.ok(DocFormResponse.from(approvalService.findForm(code)));
    }

    /** 내가 이 양식으로 상신하면 누가 결재하는지 — 상신 전에 확인한다 (T2-2). */
    @GetMapping("/{code}/approval-line")
    public ApiResponse<List<ApprovalLineResponse>> approvalLine(@PathVariable String code,
                                                                Authentication authentication) {
        Long me = Long.valueOf(authentication.getName());
        Long deptId = employeeService.findDeptIdById(me).orElse(null);

        return ApiResponse.ok(approvalService.previewLine(code, deptId).stream()
                .map(line -> ApprovalLineResponse.of(line,
                        employeeService.findNameById(line.getApproverId()).orElse(null)))
                .toList());
    }
}
