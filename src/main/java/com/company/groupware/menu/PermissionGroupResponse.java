package com.company.groupware.menu;

import java.util.List;

/** 권한 그룹 + 이 그룹에 열려 있는 메뉴 id */
public record PermissionGroupResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean isDefault,
        List<Long> menuIds,
        int memberCount
) {
}
