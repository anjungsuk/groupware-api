package com.company.groupware.approval;

/** 결재 유형 — TRD §4.2 */
public enum LineType {

    /** 순차 승인 — 순번대로 진행한다 */
    APPROVAL,

    /** 합의(병렬) — 승인 라인과 별개로 진행한다 */
    AGREEMENT,

    /** 전결 — 이 단계에서 승인하면 이후 단계를 건너뛴다 */
    DELEGATED,

    /** 후결 — 선처리 후 사후 승인 */
    POST
}
