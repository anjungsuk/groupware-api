-- 로컬 개발 전용 양식 시드 — local 프로파일에서만 로드된다.
--
-- 기본결재선은 PRD §5 확정 정책 그대로다: 신청자 → 차장(1차) → 실장(최종).
-- 반면 field_schema(양식 필드 정의)는 아직 확정된 바가 없어 자리만 잡아 둔 값이다.
-- 양식 관리(T2-1)와 휴가 양식(T2-5)에서 실제 정의가 들어오면 이 시드는 지운다.
-- 그래서 운영 마이그레이션(db/migration)이 아니라 로컬 시드에 둔다.

INSERT INTO doc_forms (code, name, version, field_schema, default_line, active, created_at, updated_at)
SELECT 'LEAVE', '휴가 신청', 1,
       '{"fields":[{"name":"leaveType","label":"휴가 종류","type":"select"},
                   {"name":"startDate","label":"시작일","type":"date"},
                   {"name":"endDate","label":"종료일","type":"date"},
                   {"name":"reason","label":"사유","type":"text"}]}',
       '{"steps":[{"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"APPROVAL"},
                  {"step":2,"positionCode":"DIRECTOR","type":"APPROVAL"}]}',
       TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM doc_forms WHERE code = 'LEAVE' AND version = 1);
