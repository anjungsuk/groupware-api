package com.company.groupware.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 반려 — 사유는 필수다 (TRD §4.2) */
public record RejectDocRequest(
        @NotBlank(message = "반려 사유를 입력해 주세요.")
        @Size(max = 500, message = "반려 사유는 500자 이내입니다.")
        String reason
) {
}
