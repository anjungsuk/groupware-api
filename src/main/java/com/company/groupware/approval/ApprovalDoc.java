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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * 결재 문서 — TRD §3.1 ApprovalDoc.
 *
 * 상태 전이(TRD §4.1)는 전부 이 클래스가 소유한다. 서비스나 컨트롤러가 status 를 직접
 * 바꾸지 못하게 setter 를 두지 않는다. 잘못된 전이는 예외로 끊는다.
 *
 * 다른 모듈(employee, doc form)은 FK id 로만 참조한다 — 모듈 경계를 넘는 연관을 만들지 않는다.
 */
@Entity
@Table(name = "approval_docs")
public class ApprovalDoc extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상신 시 채번한다. 임시저장 상태에서는 null 이다. */
    @Column(name = "doc_no", length = 20)
    private String docNo;

    @Column(name = "form_id", nullable = false)
    private Long formId;

    @Column(name = "drafter_id", nullable = false)
    private Long drafterId;

    @Column(name = "dept_id")
    private Long deptId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocStatus status;

    @Column(nullable = false, length = 200)
    private String title;

    /** 양식별 가변 필드 (TRD §3.2 하이브리드) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ApprovalDoc() {
    }

    private ApprovalDoc(Long formId, Long drafterId, Long deptId, String title, String content) {
        this.formId = formId;
        this.drafterId = drafterId;
        this.deptId = deptId;
        this.title = title;
        this.content = content;
        this.status = DocStatus.DRAFT;
    }

    /** 임시저장으로 문서를 만든다. 문서번호는 상신할 때 붙는다. */
    public static ApprovalDoc draft(Long formId, Long drafterId, Long deptId,
                                    String title, String content) {
        return new ApprovalDoc(formId, drafterId, deptId, title, content);
    }

    /**
     * 상신 — 임시저장·반려·회수 문서를 결재선에 올린다.
     * 재상신이면 이전 반려 사유를 지운다. 문서번호는 최초 상신에만 채번한다.
     */
    public void submit(String issuedDocNo) {
        if (!status.isSubmittable()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "상신할 수 없는 상태입니다: " + status);
        }

        if (docNo == null) {
            this.docNo = issuedDocNo;
        }
        this.rejectReason = null;
        this.status = DocStatus.IN_PROGRESS;
        this.submittedAt = Instant.now();
    }

    /** 최종 승인 — 종착 상태로 간다. */
    public void complete() {
        requireInFlight("완료");
        this.status = DocStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /** 반려 — 사유가 반드시 있어야 한다 (TRD §4.2). */
    public void reject(String reason) {
        requireInFlight("반려");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "반려 사유를 입력해 주세요.");
        }
        this.rejectReason = reason;
        this.status = DocStatus.REJECTED;
    }

    /**
     * 회수 — 상신자만, 최종 승인 전까지 (TRD §4.2).
     * 완료된 문서는 isInFlight 검사에서 걸린다.
     */
    public void withdraw(Long requesterId) {
        requireInFlight("회수");
        if (!drafterId.equals(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "상신자만 회수할 수 있습니다.");
        }
        this.status = DocStatus.WITHDRAWN;
    }

    /** 임시저장 상태에서만 내용을 고칠 수 있다. 진행 중 문서를 바꾸면 결재자가 본 것과 달라진다. */
    public void editDraft(String title, String content) {
        if (status != DocStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "임시저장 문서만 수정할 수 있습니다: " + status);
        }
        this.title = title;
        this.content = content;
    }

    private void requireInFlight(String action) {
        if (!status.isInFlight()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    action + "할 수 없는 상태입니다: " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public String getDocNo() {
        return docNo;
    }

    public Long getFormId() {
        return formId;
    }

    public Long getDrafterId() {
        return drafterId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public DocStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
