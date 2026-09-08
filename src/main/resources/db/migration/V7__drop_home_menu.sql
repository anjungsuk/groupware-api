-- Flyway V7: 홈 메뉴 제거
--
-- 로고를 누르면 홈으로 가는 것이 관례다. GNB 에 별도 홈 항목을 두는 화면은 드물다.
-- 홈 라우트는 메뉴 가드 밖에 있어(RequireMenu 미적용) 메뉴가 없어도 누구나 들어갈 수 있다.

DELETE FROM group_menus WHERE menu_id IN (SELECT id FROM menus WHERE code = 'HOME');
DELETE FROM menus WHERE code = 'HOME';
