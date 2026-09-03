-- Flyway V3: 결재 엔진 기반 — TRD §3.1 / §4
--
-- 범위: 문서 상태 머신(T1-1)이 필요로 하는 테이블만 만든다.
--   Vendor(T0-3) · Attachment(T2-7) · LeaveRecord(T2-5) · AuditLog(T3-3) 은
--   각 태스크에서 형태가 정해지므로 여기서 미리 만들지 않는다.
--   특히 Attachment 는 파일 용량·확장자 정책이 아직 미확정이다(03_Tasks 블로커).

-- ── 양식 ──────────────────────────────────────────────────────────
-- 공통 필드는 정규 컬럼, 양식별 가변 필드는 JSONB (TRD §3.2 하이브리드)
CREATE TABLE IF NOT EXISTS doc_forms (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    version       INT          NOT NULL DEFAULT 1,
    field_schema  JSONB        NOT NULL,
    default_line  JSONB        NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL,
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ  NOT NULL,
    updated_by    VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_doc_forms_code_version ON doc_forms (code, version);

-- ── 문서번호 채번 ─────────────────────────────────────────────────
-- TRD §4.3 은 DocSequence 테이블을 명시하지만, 접두어가 SH 하나뿐이고
-- PostgreSQL 시퀀스가 그 자체로 원자적이라 사번(employee_no_seq)과 같은 방식을 쓴다.
-- 접두어가 여러 개로 늘면 그때 테이블로 바꾼다.
CREATE SEQUENCE IF NOT EXISTS doc_no_seq START WITH 1 INCREMENT BY 1;

-- ── 문서 ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS approval_docs (
    id             BIGSERIAL PRIMARY KEY,

    -- 임시저장 상태에서는 아직 번호가 없다. 상신 시 채번한다.
    doc_no         VARCHAR(20),

    form_id        BIGINT       NOT NULL REFERENCES doc_forms (id),
    drafter_id     BIGINT       NOT NULL REFERENCES employees (id),
    dept_id        BIGINT       REFERENCES depts (id),

    status         VARCHAR(20)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content        JSONB        NOT NULL,

    -- 반려는 사유가 필수다 (TRD §4.2)
    reject_reason  VARCHAR(500),

    submitted_at   TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,

    created_at     TIMESTAMPTZ  NOT NULL,
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ  NOT NULL,
    updated_by     VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_approval_docs_doc_no
    ON approval_docs (doc_no) WHERE doc_no IS NOT NULL;

-- 문서함 조회용 (TRD §7 성능: 상태·상신자·순번 인덱스)
CREATE INDEX IF NOT EXISTS idx_approval_docs_status ON approval_docs (status);
CREATE INDEX IF NOT EXISTS idx_approval_docs_drafter ON approval_docs (drafter_id, status);

-- ── 결재선 ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS approval_lines (
    id           BIGSERIAL PRIMARY KEY,
    doc_id       BIGINT       NOT NULL REFERENCES approval_docs (id) ON DELETE CASCADE,

    -- 순번. 합의(병렬)는 같은 순번에 여러 결재자가 놓인다.
    step         INT          NOT NULL,
    approver_id  BIGINT       NOT NULL REFERENCES employees (id),

    line_type    VARCHAR(20)  NOT NULL,
    result       VARCHAR(20)  NOT NULL,
    acted_at     TIMESTAMPTZ,
    comment      VARCHAR(500),

    created_at   TIMESTAMPTZ  NOT NULL,
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ  NOT NULL,
    updated_by   VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_approval_lines_doc_step_approver
    ON approval_lines (doc_id, step, approver_id);

-- 결재 대기함 조회용
CREATE INDEX IF NOT EXISTS idx_approval_lines_approver ON approval_lines (approver_id, result);
