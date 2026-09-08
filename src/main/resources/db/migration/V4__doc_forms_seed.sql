-- Flyway V4: 결재 양식 마스터 — PRD §4.5 확정 양식 ① 휴가 신청 ② 사고 보고
--
-- 화면(T2-2)이 field_schema 를 읽어 폼을 그리므로 더는 로컬 시드로 둘 수 없다.
-- 양식 등록·버전 관리 화면(T2-1)이 생기면 이 데이터를 그 화면이 관리한다.
--
-- 기본결재선은 PRD §5 확정: 신청자 → 차장(1차) → 실장(최종).
-- 사고 보고의 후결·합의(정산·영업)는 T1-4/T1-6 이 붙은 뒤 결재선에 추가한다.

INSERT INTO doc_forms (code, name, version, field_schema, default_line, active, created_at, updated_at)
SELECT 'LEAVE', '휴가 신청', 1,
$${"fields":[
  {"name":"leaveType","label":"휴가 종류","type":"select","required":true,
   "options":[{"value":"ANNUAL","label":"연차"},
              {"value":"HALF_AM","label":"오전 반차"},
              {"value":"HALF_PM","label":"오후 반차"},
              {"value":"SICK","label":"병가"},
              {"value":"CONDOLENCE","label":"경조사"}]},
  {"name":"startDate","label":"시작일","type":"date","required":true},
  {"name":"endDate","label":"종료일","type":"date","required":true},
  {"name":"reason","label":"사유","type":"textarea","required":true}
]}$$,
$${"steps":[{"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"APPROVAL"},
            {"step":2,"positionCode":"DIRECTOR","type":"APPROVAL"}]}$$,
       TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM doc_forms WHERE code = 'LEAVE' AND version = 1);

INSERT INTO doc_forms (code, name, version, field_schema, default_line, active, created_at, updated_at)
SELECT 'ACCIDENT', '사고 보고', 1,
$${"fields":[
  {"name":"accidentType","label":"사고 유형","type":"select","required":true,
   "options":[{"value":"LOSS","label":"손망실"},
              {"value":"MISDELIVERY","label":"오배송"}]},
  {"name":"shipperName","label":"화주사","type":"text","required":true},
  {"name":"trackingNo","label":"운송장 번호","type":"text","required":true},
  {"name":"occurredOn","label":"발생일","type":"date","required":true},
  {"name":"description","label":"사고 내용","type":"textarea","required":true},
  {"name":"action","label":"조치 사항","type":"textarea","required":false}
]}$$,
$${"steps":[{"step":1,"positionCode":"DEPUTY_GENERAL_MANAGER","type":"APPROVAL"},
            {"step":2,"positionCode":"DIRECTOR","type":"APPROVAL"}]}$$,
       TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM doc_forms WHERE code = 'ACCIDENT' AND version = 1);
