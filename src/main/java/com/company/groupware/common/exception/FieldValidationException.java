package com.company.groupware.common.exception;

import java.util.Map;

/**
 * 필드 단위 검증 실패. 프론트가 해당 입력 옆에 인라인으로 표시할 수 있도록
 * `C001` + `data.{필드}` 형태로 응답한다 (docs/04_인증_API_명세.md §1).
 * 중복 이메일처럼 DB 를 봐야 알 수 있는 검증에 쓴다.
 */
public class FieldValidationException extends BusinessException {

    private final Map<String, String> fieldErrors;

    public FieldValidationException(Map<String, String> fieldErrors) {
        super(ErrorCode.INVALID_INPUT);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public static FieldValidationException of(String field, String message) {
        return new FieldValidationException(Map.of(field, message));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
