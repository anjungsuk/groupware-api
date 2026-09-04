package com.company.groupware.approval;

import com.company.groupware.common.response.ApiResponse;
import com.company.groupware.employee.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 결재 처리 API — TRD §5.
 * 권한·현재 결재순번·상태는 전부 ApprovalService 가 서버에서 검증한다.
 */
@RestController
@RequestMapping("/api/v1/docs")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;
    private final EmployeeService employeeService;

    /** 문서함 — TRD §5 `GET /docs?box=`. 기본은 결재 대기함이다. */
    @GetMapping
    public ApiResponse<List<ApprovalDocSummary>> box(
            @RequestParam(defaultValue = "PENDING") DocBox box,
            Authentication authentication) {
        Long me = currentEmployeeId(authentication);

        return ApiResponse.ok(approvalService.findBox(box, me).stream()
                .map(doc -> ApprovalDocSummary.of(doc, nameOf(doc.getDrafterId())))
                .toList());
    }

    /** 결재 대기함 (box=PENDING 과 같다. 기존 경로를 유지한다) */
    @GetMapping("/pending")
    public ApiResponse<List<ApprovalDocSummary>> pending(Authentication authentication) {
        return box(DocBox.PENDING, authentication);
    }

    /** 문서 상세·진행현황 */
    @GetMapping("/{id}")
    public ApiResponse<ApprovalDocResponse> detail(@PathVariable Long id) {
        ApprovalDoc doc = approvalService.findDoc(id);
        return ApiResponse.ok(ApprovalDocResponse.of(doc, lineResponses(id)));
    }

    /** 임시저장 생성. 결재선은 상신 시점에 만들어진다. */
    @PostMapping
    public ApiResponse<ApprovalDocResponse> create(@Valid @RequestBody CreateDocRequest request,
                                                   Authentication authentication) {
        Long me = currentEmployeeId(authentication);
        ApprovalDoc doc = approvalService.createDraft(
                request.formCode(), me, employeeService.findDeptIdById(me).orElse(null),
                request.title(), request.content());

        return ApiResponse.ok(ApprovalDocResponse.of(doc, List.of()));
    }

    /** 임시저장 수정 */
    @PutMapping("/{id}")
    public ApiResponse<ApprovalDocResponse> edit(@PathVariable Long id,
                                                 @Valid @RequestBody CreateDocRequest request,
                                                 Authentication authentication) {
        ApprovalDoc doc = approvalService.editDraft(
                id, currentEmployeeId(authentication), request.title(), request.content());
        return ApiResponse.ok(ApprovalDocResponse.of(doc, lineResponses(id)));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ApprovalDocResponse> submit(@PathVariable Long id,
                                                   Authentication authentication) {
        ApprovalDoc doc = approvalService.submit(id, currentEmployeeId(authentication));
        return ApiResponse.ok(ApprovalDocResponse.of(doc, lineResponses(id)));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ApprovalDocResponse> approve(@PathVariable Long id,
                                                    @Valid @RequestBody ApproveDocRequest request,
                                                    Authentication authentication) {
        ApprovalDoc doc = approvalService.approve(id, currentEmployeeId(authentication), request.comment());
        return ApiResponse.ok(ApprovalDocResponse.of(doc, lineResponses(id)));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ApprovalDocResponse> reject(@PathVariable Long id,
                                                   @Valid @RequestBody RejectDocRequest request,
                                                   Authentication authentication) {
        ApprovalDoc doc = approvalService.reject(id, currentEmployeeId(authentication), request.reason());
        return ApiResponse.ok(ApprovalDocResponse.of(doc, lineResponses(id)));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<ApprovalDocResponse> withdraw(@PathVariable Long id,
                                                     Authentication authentication) {
        ApprovalDoc doc = approvalService.withdraw(id, currentEmployeeId(authentication));
        return ApiResponse.ok(ApprovalDocResponse.of(doc, lineResponses(id)));
    }

    private List<ApprovalLineResponse> lineResponses(Long docId) {
        return approvalService.findLines(docId).stream()
                .map(line -> ApprovalLineResponse.of(line, nameOf(line.getApproverId())))
                .toList();
    }

    private String nameOf(Long employeeId) {
        return employeeService.findNameById(employeeId).orElse(null);
    }

    /** JWT subject 가 사원 id 다 (AuthService 가 그렇게 넣는다). */
    private Long currentEmployeeId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
