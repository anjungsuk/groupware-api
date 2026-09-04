package com.company.groupware.menu;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** 사원이 속할 그룹 목록. 비우면 기본 그룹이 적용된다. */
public record GroupAssignRequest(
        @NotNull(message = "그룹 목록이 필요합니다.")
        List<Long> groupIds
) {
}
