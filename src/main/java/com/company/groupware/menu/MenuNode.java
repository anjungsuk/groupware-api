package com.company.groupware.menu;

import java.util.List;

/**
 * 화면에 내려보내는 메뉴 트리.
 * 상위(GNB)가 children(LNB)을 갖는다. 하위가 없는 상위는 자기 path 로 간다.
 */
public record MenuNode(
        Long id,
        String code,
        String name,
        String path,
        List<MenuNode> children
) {
}
