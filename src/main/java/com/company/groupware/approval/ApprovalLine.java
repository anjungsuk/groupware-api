package com.company.groupware.approval;

import com.company.groupware.common.entity.BaseEntity;
import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 결재선 한 단계 — TRD §3.1 ApprovalLine.
 * 합의(병렬)는 같은 step 에 여러 행이 놓인다.
 */
@Entity
@Table(name = "approval_lines")
public class ApprovalLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(nullable = false)
    private int step;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, length = 20)
    private LineType lineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineResult result;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(length = 500)
    private String comment;

    protected ApprovalLine() {
    }

    private ApprovalLine(Long docId, int step, Long approverId, LineType lineType) {
        this.docId = docId;
        this.step = step;
        this.approverId = approverId;
        this.lineType = lineType;
        this.result = LineResult.PENDING;
    }

    /** 상신 시 생성되는 대기 상태의 결재선. */
    public static ApprovalLine pending(Long docId, int step, Long approverId, LineType lineType) {
        return new ApprovalLine(docId, step, approverId, lineType);
    }

    public void approve(String comment) {
        act(LineResult.APPROVED, comment);
    }

    public void reject(String comment) {
        act(LineResult.REJECTED, comment);
    }

    private void act(LineResult next, String comment) {
        if (result != LineResult.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "이미 처리된 결재선입니다: " + result);
        }
        this.result = next;
        this.comment = comment;
        this.actedAt = Instant.now();
    }

    public boolean isPending() {
        return result == LineResult.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Long getDocId() {
        return docId;
    }

    public int getStep() {
        return step;
    }

    public Long getApproverId() {
        return approverId;
    }

    public LineType getLineType() {
        return lineType;
    }

    public LineResult getResult() {
        return result;
    }

    public Instant getActedAt() {
        return actedAt;
    }

    public String getComment() {
        return comment;
    }
}
