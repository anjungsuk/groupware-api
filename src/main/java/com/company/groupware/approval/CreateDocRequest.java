package com.company.groupware.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 임시저장 문서 생성.
 * content 는 양식별 가변 필드라 JSON 문자열로 받는다 (TRD §3.2).
 * 필드 단위 검증은 양식 정의가 확정되는 T2-1/T2-5 소관이다.
 */
public record CreateDocRequest(
        @NotBlank(message = "양식을 선택해 주세요.")
        String formCode,

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이내입니다.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        String content
) {
}
