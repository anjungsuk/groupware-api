package com.company.groupware.approval;

import java.time.Instant;
import java.util.List;

/** 문서 상세·진행현황 — 프론트 `ApprovalDocDetail` 과 1:1 */
public record ApprovalDocResponse(
        Long id,
        String docNo,
        String title,
        String status,
        Long drafterId,
        String content,
        String rejectReason,
        Instant submittedAt,
        Instant completedAt,
        List<ApprovalLineResponse> lines
) {

    public static ApprovalDocResponse of(ApprovalDoc doc, List<ApprovalLineResponse> lines) {
        return new ApprovalDocResponse(
                doc.getId(),
                doc.getDocNo(),
                doc.getTitle(),
                doc.getStatus().name(),
                doc.getDrafterId(),
                doc.getContent(),
                doc.getRejectReason(),
                doc.getSubmittedAt(),
                doc.getCompletedAt(),
                lines);
    }
}
