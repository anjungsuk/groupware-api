package com.company.groupware.approval;

import jakarta.validation.constraints.Size;

/** 승인 — 의견은 선택 */
public record ApproveDocRequest(
        @Size(max = 500, message = "의견은 500자 이내입니다.")
        String comment
) {
}
