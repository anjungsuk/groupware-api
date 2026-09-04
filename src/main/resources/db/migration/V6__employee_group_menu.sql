-- Flyway V6: 사원 권한 메뉴 추가
--
-- 사원별 그룹 배정 화면이 생겼다. 관리자가 그 화면에 갈 수 있어야 하므로 메뉴를 심는다.
-- (메뉴 등록 화면이 있지만 자기 자신을 등록할 수는 없다 — 닭과 달걀)

INSERT INTO menus (code, name, path, parent_id, sort_order, created_at, updated_at)
SELECT 'ADMIN_EMPLOYEE_GROUP', '사원 권한', '/admin/employee-groups', p.id, 4, NOW(), NOW()
FROM menus p
WHERE p.code = 'ADMIN'
ON CONFLICT (code) DO NOTHING;

INSERT INTO group_menus (group_id, menu_id)
SELECT g.id, m.id
FROM permission_groups g, menus m
WHERE g.code = 'ADMIN' AND m.code = 'ADMIN_EMPLOYEE_GROUP'
ON CONFLICT DO NOTHING;
