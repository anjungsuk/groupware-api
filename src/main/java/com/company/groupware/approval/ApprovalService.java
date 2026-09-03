package com.company.groupware.approval;

import com.company.groupware.approval.internal.ApprovalDocRepository;
import com.company.groupware.approval.internal.ApprovalLineFactory;
import com.company.groupware.approval.internal.ApprovalLineRepository;
import com.company.groupware.approval.internal.DocFormRepository;
import com.company.groupware.approval.internal.DocNoGenerator;
import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 결재 처리 — 결재 모듈의 공식 API (T1-3, TRD §4.2).
 *
 * 순차 결재의 규칙은 하나다: <b>현재 순번의 대기 중인 결재자만</b> 처리할 수 있다.
 * 그래서 모든 처리 API 가 상태·순번·권한 셋을 함께 본다 (TRD §5 주석).
 *
 * 합의(AGREEMENT)는 승인 라인과 별개로 진행되므로(TRD §4.2) 순번 계산에서 제외한다.
 * 합의 라인의 진행 규칙 자체는 T1-4 소관이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalDocRepository docRepository;
    private final ApprovalLineRepository lineRepository;
    private final DocFormRepository formRepository;
    private final ApprovalLineFactory lineFactory;
    private final DocNoGenerator docNoGenerator;

    /** 임시저장 — 결재선은 아직 만들지 않는다. 상신 시점 조직도로 만들어야 하기 때문이다. */
    @Transactional
    public ApprovalDoc createDraft(String formCode, Long drafterId, Long drafterDeptId,
                                   String title, String content) {
        DocForm form = formRepository.findFirstByCodeAndActiveTrueOrderByVersionDesc(formCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "양식을 찾을 수 없습니다: " + formCode));

        return docRepository.save(
                ApprovalDoc.draft(form.getId(), drafterId, drafterDeptId, title, content));
    }

    /**
     * 상신 — 결재선을 만들고 문서를 진행 상태로 넘긴다.
     * 재상신이면 남아 있던 이전 결재선을 버리고 다시 만든다. 조직도가 바뀌었을 수 있다.
     */
    @Transactional
    public ApprovalDoc submit(Long docId, Long requesterId) {
        ApprovalDoc doc = findDoc(docId);
        requireDrafter(doc, requesterId, "상신");

        DocForm form = formRepository.findById(doc.getFormId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "양식을 찾을 수 없습니다."));

        // 결재선을 먼저 만든다 — 결재자를 못 찾으면 문서 상태를 건드리지 않고 실패해야 한다
        List<ApprovalLine> lines = lineFactory.create(doc.getId(), form, doc.getDeptId());

        lineRepository.deleteAll(lineRepository.findByDocIdOrderByStepAsc(docId));
        lineRepository.saveAll(lines);

        doc.submit(docNoGenerator.next());
        return doc;
    }

    /**
     * 승인 — 현재 순번의 내 라인을 승인한다.
     * 남은 승인 라인이 없으면 문서가 완료된다.
     */
    @Transactional
    public ApprovalDoc approve(Long docId, Long approverId, String comment) {
        ApprovalDoc doc = findDoc(docId);
        List<ApprovalLine> lines = lineRepository.findByDocIdOrderByStepAsc(docId);

        currentLineOf(doc, lines, approverId).approve(comment);

        if (approvalLines(lines).noneMatch(ApprovalLine::isPending)) {
            doc.complete();
        }
        return doc;
    }

    /** 반려 — 사유가 필수다. 한 단계라도 반려되면 문서 전체가 반려된다. */
    @Transactional
    public ApprovalDoc reject(Long docId, Long approverId, String reason) {
        ApprovalDoc doc = findDoc(docId);
        List<ApprovalLine> lines = lineRepository.findByDocIdOrderByStepAsc(docId);

        // 순서가 중요하다. currentLineOf 는 검증만 하고(상태·순번·권한),
        // doc.reject 가 사유를 검증한다. 둘 다 통과한 뒤에야 라인을 바꾼다.
        // 문서를 먼저 반려하면 currentLineOf 의 상태 검사가 스스로 막혀 권한 검증이 죽는다.
        ApprovalLine line = currentLineOf(doc, lines, approverId);
        doc.reject(reason);
        line.reject(reason);
        return doc;
    }

    /** 회수 — 상신자만, 최종 승인 전까지 (TRD §4.2). */
    @Transactional
    public ApprovalDoc withdraw(Long docId, Long requesterId) {
        ApprovalDoc doc = findDoc(docId);
        doc.withdraw(requesterId);
        return doc;
    }

    public ApprovalDoc findDoc(Long docId) {
        return docRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "문서를 찾을 수 없습니다."));
    }

    public List<ApprovalLine> findLines(Long docId) {
        return lineRepository.findByDocIdOrderByStepAsc(docId);
    }

    /** 결재 대기함 — 내 차례가 온 문서만. 아직 앞 순번이 남은 문서는 보이지 않는다. */
    public List<ApprovalDoc> findPendingDocs(Long approverId) {
        // ponytail: 문서마다 결재선을 다시 조회한다(N+1). 개인 결재 대기함은 건수가 적어
        //           그대로 두지만, 문서함 전체 조회(T2-3)에서는 조인 한 방으로 바꾼다.
        return lineRepository.findByApproverIdAndResultOrderByStepAsc(approverId, LineResult.PENDING)
                .stream()
                .filter(line -> isCurrentApprover(line.getDocId(), approverId))
                .map(line -> findDoc(line.getDocId()))
                .filter(doc -> doc.getStatus().isInFlight())
                .distinct()
                .toList();
    }

    private boolean isCurrentApprover(Long docId, Long approverId) {
        List<ApprovalLine> lines = lineRepository.findByDocIdOrderByStepAsc(docId);
        return currentStep(lines)
                .map(step -> approvalLines(lines)
                        .anyMatch(l -> l.getStep() == step
                                && l.isPending()
                                && l.getApproverId().equals(approverId)))
                .orElse(false);
    }

    /**
     * 지금 처리해야 할 라인을 찾는다. 셋 중 하나라도 어긋나면 거부한다.
     * 상태(진행중) · 순번(현재 순번) · 권한(그 순번의 결재자 본인).
     */
    private ApprovalLine currentLineOf(ApprovalDoc doc, List<ApprovalLine> lines, Long approverId) {
        if (!doc.getStatus().isInFlight()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "결재할 수 없는 상태입니다: " + doc.getStatus());
        }

        int step = currentStep(lines).orElseThrow(() -> new BusinessException(
                ErrorCode.INVALID_INPUT, "처리할 결재선이 없습니다."));

        return approvalLines(lines)
                .filter(line -> line.getStep() == step && line.isPending())
                .filter(line -> line.getApproverId().equals(approverId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED,
                        "현재 결재 차례가 아닙니다."));
    }

    /** 대기 중인 승인 라인 가운데 가장 앞선 순번. */
    private java.util.Optional<Integer> currentStep(List<ApprovalLine> lines) {
        return approvalLines(lines)
                .filter(ApprovalLine::isPending)
                .map(ApprovalLine::getStep)
                .min(Comparator.naturalOrder());
    }

    /** 합의는 순차 진행과 별개다 (TRD §4.2) — 순번 계산에서 뺀다. */
    private java.util.stream.Stream<ApprovalLine> approvalLines(List<ApprovalLine> lines) {
        return lines.stream().filter(line -> line.getLineType() != LineType.AGREEMENT);
    }

    private void requireDrafter(ApprovalDoc doc, Long requesterId, String action) {
        if (!doc.getDrafterId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "상신자만 " + action + "할 수 있습니다.");
        }
    }
}
