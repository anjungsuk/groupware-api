package com.company.groupware.approval;

/**
 * 문서 상태 — TRD §4.1.
 *
 * <pre>
 * DRAFT ──상신──> IN_PROGRESS ──최종 승인──> COMPLETED
 *   ^                 │
 *   │                 ├──반려──> REJECTED ──재상신──┐
 *   │                 └──회수──> WITHDRAWN ─재상신──┤
 *   └──────────────────────────────────────────────┘
 * </pre>
 *
 * COMPLETED 는 종착 상태다. 완료된 문서는 어떤 전이도 받지 않는다.
 */
public enum DocStatus {

    /** 임시저장 — 아직 문서번호가 없다 */
    DRAFT,

    /** 진행중 — 상신되어 결재선을 타고 있다 */
    IN_PROGRESS,

    /** 완료 — 최종 승인. 종착 상태 */
    COMPLETED,

    /** 반려 — 사유가 반드시 남는다. 재상신할 수 있다 */
    REJECTED,

    /** 회수 — 상신자가 거둬들였다. 재상신할 수 있다 */
    WITHDRAWN;

    /** 상신할 수 있는 상태인가. 반려·회수 문서의 재상신을 포함한다. */
    public boolean isSubmittable() {
        return this == DRAFT || this == REJECTED || this == WITHDRAWN;
    }

    /** 결재선이 살아 있어 승인·반려·회수를 받을 수 있는 상태인가. */
    public boolean isInFlight() {
        return this == IN_PROGRESS;
    }
}
