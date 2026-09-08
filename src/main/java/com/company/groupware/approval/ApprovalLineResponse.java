package com.company.groupware.approval;

import java.time.Instant;

/** 결재선 한 단계 — 진행현황 표시용 */
public record ApprovalLineResponse(
        Long id,
        int step,
        Long approverId,
        String approverName,
        String lineType,
        String result,
        Instant actedAt,
        String comment
) {

    public static ApprovalLineResponse of(ApprovalLine line, String approverName) {
        return new ApprovalLineResponse(
                line.getId(),
                line.getStep(),
                line.getApproverId(),
                approverName,
                line.getLineType().name(),
                line.getResult().name(),
                line.getActedAt(),
                line.getComment());
    }
}
