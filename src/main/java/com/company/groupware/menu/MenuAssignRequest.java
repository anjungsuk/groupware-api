package com.company.groupware.menu;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 그룹에 열어 줄 메뉴 목록. 빈 목록은 "아무것도 열지 않음" 이라 허용한다. */
public record MenuAssignRequest(
        @NotNull(message = "메뉴 목록이 필요합니다.")
        List<Long> menuIds
) {
}
