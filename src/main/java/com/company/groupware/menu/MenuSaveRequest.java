package com.company.groupware.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 메뉴 등록·수정.
 *
 * path 는 프론트 라우트와 맞아야 한다. 서버는 형식만 보고, 실재하는 경로인지는
 * 관리 화면이 목록으로 제한한다 — 어떤 경로가 있는지는 프론트만 안다.
 */
public record MenuSaveRequest(
        @NotBlank(message = "코드를 입력해 주세요.")
        @Size(max = 40)
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "코드는 영문 대문자·숫자·밑줄만 씁니다.")
        String code,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 60, message = "이름은 60자 이내입니다.")
        String name,

        /** 대분류는 갈 곳이 없어 비워 둔다. */
        @Size(max = 200)
        String path,

        Long parentId,

        @PositiveOrZero(message = "정렬 순서는 0 이상입니다.")
        int sortOrder,

        boolean active
) {
}
