package com.company.groupware.approval;

import java.time.Instant;

/** 문서함 목록용 요약 */
public record ApprovalDocSummary(
        Long id,
        String docNo,
        String title,
        String status,
        Long drafterId,
        String drafterName,
        Instant submittedAt
) {

    public static ApprovalDocSummary of(ApprovalDoc doc, String drafterName) {
        return new ApprovalDocSummary(
                doc.getId(),
                doc.getDocNo(),
                doc.getTitle(),
                doc.getStatus().name(),
                doc.getDrafterId(),
                drafterName,
                doc.getSubmittedAt());
    }
}
