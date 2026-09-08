package com.company.groupware.menu;

/** 메뉴 관리 화면용 평면 목록 */
public record MenuResponse(
        Long id,
        String code,
        String name,
        String path,
        Long parentId,
        int sortOrder,
        boolean active
) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getCode(), menu.getName(), menu.getPath(),
                menu.getParentId(), menu.getSortOrder(), menu.isActive());
    }
}
