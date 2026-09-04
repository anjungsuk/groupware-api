package com.company.groupware.approval;

/**
 * 문서함 — TRD §5 `GET /docs?box=`.
 *
 * PRD 는 5종(상신/임시저장/결재대기/완료/수신·참조)을 말하지만 수신·참조는
 * 참조자 개념이 아직 없어 뺐다. 참조 기능이 생기면 여기에 RECEIVED 를 더한다.
 */
public enum DocBox {

    /** 임시저장 — 내가 쓰다 만 문서 */
    DRAFT,

    /** 상신함 — 내가 올린 문서 전부 */
    SENT,

    /** 결재대기 — 내 차례가 온 문서 */
    PENDING,

    /** 완료 — 내가 올렸고 결재가 끝난 문서 */
    DONE
}
