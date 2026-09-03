-- 로컬 개발 전용 시드 — local 프로파일에서만 로드된다 (application-local.yml 의 flyway.locations).
-- 최초 관리자가 없으면 아무도 가입을 승인할 수 없어 로그인이 불가능하므로 하나 심어 둔다.
--
-- 계정: admin@company.co.kr / admin1234  (BCrypt, cost 10)
-- 운영·개발 서버에는 절대 올리지 않는다. 그쪽은 수동 INSERT 로 만든다.
--
-- 사번 EMP-2026-0000 은 employee_no_seq 를 소비하지 않는 예약 번호다.

INSERT INTO employees (employee_no, name, email, password, birth_date,
                       zip_code, address, address_detail,
                       mobile_phone, emergency_relation, emergency_phone,
                       status, role, dept_id, position_code, hire_date, approved_at,
                       deleted, created_at, updated_at)
SELECT 'EMP-2026-0000', '시스템관리자', 'admin@company.co.kr',
       '$2a$10$wdXpil/EifWCzZEaedP5bu7oQLgExzMrMl/CwFQSA/2ks.zwslcqG',
       DATE '1980-01-01',
       '06236', '서울 강남구 테헤란로 1', '1층',
       '010-0000-0000', 'SPOUSE', '010-0000-0000',
       'ACTIVE', 'SUPER_ADMIN', 1, 'CEO', DATE '2020-01-01', NOW(),
       FALSE, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM employees WHERE email = 'admin@company.co.kr' AND deleted = FALSE
);
