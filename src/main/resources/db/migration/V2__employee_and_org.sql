-- Flyway V2: 사원(Employee) · 조직(Dept) · 직급(Position)
-- TRD §3.1 Employee/Dept 기준. 회원가입은 PENDING 으로 생성되고 관리자 승인 후 ACTIVE 가 된다.
--
-- 주의: V1 의 users 테이블은 이 마이그레이션에서 건드리지 않는다.
--       employees 로 대체되었으므로 데이터가 없음을 확인한 뒤 별도 마이그레이션으로 정리할 것.

-- ── 부서 ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS depts (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT REFERENCES depts (id),
    created_at  TIMESTAMPTZ NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ NOT NULL,
    updated_by  VARCHAR(100)
);

-- ── 직급 ──────────────────────────────────────────────────────────
-- PRD §5 결재선: 차장(DEPUTY_GENERAL_MANAGER)=1차 승인자, 실장(DIRECTOR)=최종 승인자
CREATE TABLE IF NOT EXISTS positions (
    code        VARCHAR(40) PRIMARY KEY,
    name        VARCHAR(40) NOT NULL,
    sort_order  INT         NOT NULL
);

-- ── 사번 채번 ─────────────────────────────────────────────────────
-- 형식: EMP-{연도}-{4자리}. 시퀀스는 연도와 무관하게 전역 증가한다.
CREATE SEQUENCE IF NOT EXISTS employee_no_seq START WITH 1 INCREMENT BY 1;

-- ── 사원 ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
    id                  BIGSERIAL PRIMARY KEY,
    employee_no         VARCHAR(20)  NOT NULL,
    name                VARCHAR(50)  NOT NULL,
    email               VARCHAR(100) NOT NULL,
    password            VARCHAR(255) NOT NULL,
    birth_date          DATE         NOT NULL,

    zip_code            VARCHAR(10)  NOT NULL,
    address             VARCHAR(255) NOT NULL,
    address_detail      VARCHAR(100) NOT NULL,

    mobile_phone        VARCHAR(20)  NOT NULL,
    home_phone          VARCHAR(20),

    emergency_relation  VARCHAR(20)  NOT NULL,
    emergency_phone     VARCHAR(20)  NOT NULL,

    status              VARCHAR(20)  NOT NULL,
    role                VARCHAR(20)  NOT NULL,

    -- 관리자가 승인 시점에 배정한다
    dept_id             BIGINT REFERENCES depts (id),
    position_code       VARCHAR(40) REFERENCES positions (code),
    hire_date           DATE,
    approved_at         TIMESTAMPTZ,

    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100),
    created_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ  NOT NULL,
    updated_by          VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_employees_email_active
    ON employees (email) WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_employees_employee_no
    ON employees (employee_no);

-- 승인 대기 목록 조회용
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees (status);

-- ── 초기 마스터 데이터 ────────────────────────────────────────────
INSERT INTO depts (id, name, parent_id, created_at, updated_at)
VALUES (1, '경영지원실', NULL, NOW(), NOW()),
       (2, '물류운영팀', 1, NOW(), NOW()),
       (3, '정산팀', 1, NOW(), NOW()),
       (4, '영업팀', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval('depts_id_seq', (SELECT MAX(id) FROM depts));

INSERT INTO positions (code, name, sort_order)
VALUES ('STAFF', '사원', 1),
       ('SENIOR_STAFF', '주임', 2),
       ('ASSISTANT_MANAGER', '대리', 3),
       ('MANAGER', '과장', 4),
       ('DEPUTY_GENERAL_MANAGER', '차장', 5),
       ('GENERAL_MANAGER', '부장', 6),
       ('DIRECTOR', '실장', 7),
       ('CEO', '대표이사', 8)
ON CONFLICT (code) DO NOTHING;
