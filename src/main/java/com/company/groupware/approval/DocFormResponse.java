package com.company.groupware.approval;

/**
 * 양식 — 화면이 field_schema 를 읽어 폼을 그린다 (TRD §3.2 하이브리드).
 * 필드 정의는 양식마다 달라 JSON 문자열 그대로 내려보낸다.
 */
public record DocFormResponse(
        Long id,
        String code,
        String name,
        int version,
        String fieldSchema
) {

    public static DocFormResponse from(DocForm form) {
        return new DocFormResponse(form.getId(), form.getCode(), form.getName(),
                form.getVersion(), form.getFieldSchema());
    }
}
