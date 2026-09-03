package com.company.groupware.approval;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import com.company.groupware.employee.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 양식의 기본결재선 → 실제 결재선 (T1-2, TRD §4.2).
 *
 * 기본결재선은 직급으로만 적혀 있다. 상신 시점의 조직도에서 실제 결재자를 찾아
 * ApprovalLine 을 만든다 — 그래서 인사이동이 있어도 양식은 그대로 둘 수 있다.
 *
 * 합의(병렬)·전결·후결의 진행 규칙은 T1-4~T1-6 소관이다. 여기서는 정의된 단계를
 * 그대로 결재선으로 옮기기만 한다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalLineFactory {

    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    /**
     * @param drafterDeptId 상신자의 부서. 여기서부터 상위로 올라가며 결재자를 찾는다.
     */
    public List<ApprovalLine> create(Long docId, DocForm form, Long drafterDeptId) {
        if (drafterDeptId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "부서가 배정되지 않아 결재선을 만들 수 없습니다.");
        }

        List<DefaultLine.Step> steps = parse(form).steps();
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "기본 결재선이 비어 있습니다: " + form.getCode());
        }

        List<ApprovalLine> lines = new ArrayList<>();
        for (DefaultLine.Step step : steps.stream()
                .sorted(Comparator.comparingInt(DefaultLine.Step::step))
                .toList()) {

            Long approverId = employeeService
                    .findApproverId(drafterDeptId, step.positionCode())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORG_NOT_FOUND,
                            "결재선의 " + step.positionCode() + " 을(를) 조직도에서 찾지 못했습니다."));

            lines.add(ApprovalLine.pending(docId, step.step(), approverId, step.type()));
        }
        return lines;
    }

    private DefaultLine parse(DocForm form) {
        try {
            return objectMapper.readValue(form.getDefaultLine(), DefaultLine.class);
        } catch (Exception e) {
            // 양식 등록 시 검증되지만, 잘못 저장된 JSON 이 500 으로 새어 나가지 않게 막는다
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "기본 결재선 정의를 읽을 수 없습니다: " + form.getCode());
        }
    }
}
