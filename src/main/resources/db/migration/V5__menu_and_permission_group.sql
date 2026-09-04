-- Flyway V5: 메뉴 · 권한 그룹
--
-- 메뉴 노출 권한을 코드에서 데이터로 옮긴다. 지금까지는 프론트에 adminOnly 가 박혀 있어
-- 누가 무엇을 보는지 바꾸려면 프론트를 다시 배포해야 했다.
--
-- 사원 → 그룹 → 메뉴 3단이다. 사원은 여러 그룹에 속할 수 있고 메뉴는 합집합으로 열린다.
--
-- 주의: 메뉴의 path 는 프론트 라우트와 맞아야 한다. 서버는 문자열만 저장하므로
--       메뉴 관리 화면이 등록 가능한 경로를 목록으로 제한한다(자유 입력 금지).

-- ── 메뉴 ──────────────────────────────────────────────────────────
-- 2단 구조: 상위(GNB 대분류)는 path 가 없고, 하위(LNB 항목)가 path 를 갖는다.
CREATE TABLE IF NOT EXISTS menus (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    path        VARCHAR(200),
    parent_id   BIGINT       REFERENCES menus (id),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ  NOT NULL,
    updated_by  VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_menus_code ON menus (code);
CREATE INDEX IF NOT EXISTS idx_menus_parent ON menus (parent_id, sort_order);

-- ── 권한 그룹 ─────────────────────────────────────────────────────
-- is_default: 아무 그룹에도 안 속한 사원이 받는 그룹. 신규 가입자를 따로 배정하지
--             않아도 최소 메뉴가 열린다. 하나만 true 여야 한다.
CREATE TABLE IF NOT EXISTS permission_groups (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(40)  NOT NULL,
    name         VARCHAR(60)  NOT NULL,
    description  VARCHAR(200),
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at   TIMESTAMPTZ  NOT NULL,
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ  NOT NULL,
    updated_by   VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_permission_groups_code ON permission_groups (code);
CREATE UNIQUE INDEX IF NOT EXISTS ux_permission_groups_default
    ON permission_groups (is_default) WHERE is_default = TRUE;

-- ── 사원 ↔ 그룹 ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employee_groups (
    employee_id  BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    group_id     BIGINT NOT NULL REFERENCES permission_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (employee_id, group_id)
);

CREATE INDEX IF NOT EXISTS idx_employee_groups_group ON employee_groups (group_id);

-- ── 그룹 ↔ 메뉴 ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS group_menus (
    group_id  BIGINT NOT NULL REFERENCES permission_groups (id) ON DELETE CASCADE,
    menu_id   BIGINT NOT NULL REFERENCES menus (id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, menu_id)
);

-- ── 초기 데이터 ───────────────────────────────────────────────────
-- 지금 화면에 있는 메뉴를 그대로 옮긴다. 경로는 프론트 라우트와 일치한다.
INSERT INTO menus (code, name, path, parent_id, sort_order, created_at, updated_at)
VALUES ('HOME', '홈', '/', NULL, 1, NOW(), NOW()),
       ('APPROVAL', '전자결재', NULL, NULL, 2, NOW(), NOW()),
       ('ADMIN', '관리', NULL, NULL, 3, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO menus (code, name, path, parent_id, sort_order, created_at, updated_at)
SELECT v.code, v.name, v.path, p.id, v.sort_order, NOW(), NOW()
FROM (VALUES ('APPROVAL_NEW',     '결재 작성',   '/docs/new',        'APPROVAL', 1),
             ('APPROVAL_PENDING', '결재 대기',   '/approvals',       'APPROVAL', 2),
             ('APPROVAL_DRAFT',   '임시저장',    '/docs/draft',      'APPROVAL', 3),
             ('APPROVAL_SENT',    '상신함',      '/docs/sent',       'APPROVAL', 4),
             ('APPROVAL_DONE',    '완료',        '/docs/done',       'APPROVAL', 5),
             ('ADMIN_SIGNUP',     '가입 승인',   '/admin/employees', 'ADMIN',    1),
             ('ADMIN_MENU',       '메뉴 관리',   '/admin/menus',     'ADMIN',    2),
             ('ADMIN_GROUP',      '권한 그룹',   '/admin/groups',    'ADMIN',    3)
     ) AS v(code, name, path, parent_code, sort_order)
JOIN menus p ON p.code = v.parent_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO permission_groups (code, name, description, is_default, created_at, updated_at)
VALUES ('MEMBER', '일반 사원', '결재 작성·조회', TRUE, NOW(), NOW()),
       ('ADMIN', '시스템 관리자', '전체 메뉴', FALSE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

-- 일반 사원: 홈 + 전자결재 전체
INSERT INTO group_menus (group_id, menu_id)
SELECT g.id, m.id
FROM permission_groups g, menus m
WHERE g.code = 'MEMBER'
  AND (m.code IN ('HOME', 'APPROVAL') OR m.code LIKE 'APPROVAL\_%')
ON CONFLICT DO NOTHING;

-- 시스템 관리자: 전체
INSERT INTO group_menus (group_id, menu_id)
SELECT g.id, m.id
FROM permission_groups g, menus m
WHERE g.code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 시드 관리자 계정을 관리자 그룹에 넣는다. 나머지는 기본 그룹으로 자동 처리된다.
INSERT INTO employee_groups (employee_id, group_id)
SELECT e.id, g.id
FROM employees e, permission_groups g
WHERE e.email = 'admin@company.co.kr' AND g.code = 'ADMIN'
ON CONFLICT DO NOTHING;
