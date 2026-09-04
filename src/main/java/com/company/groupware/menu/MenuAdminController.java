package com.company.groupware.menu;

import com.company.groupware.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메뉴·권한 그룹 관리 — 메뉴 노출 정책을 화면에서 바꾼다.
 * 코드 배포 없이 누가 무엇을 보는지 조정하는 것이 이 기능의 목적이다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class MenuAdminController {

    private final MenuService menuService;

    @GetMapping("/menus")
    public ApiResponse<List<MenuResponse>> menus() {
        return ApiResponse.ok(menuService.findAllMenus().stream().map(MenuResponse::from).toList());
    }

    @GetMapping("/groups")
    public ApiResponse<List<PermissionGroupResponse>> groups() {
        return ApiResponse.ok(menuService.findGroups());
    }

    /** 그룹에 열어 줄 메뉴를 통째로 교체한다. */
    @PutMapping("/groups/{id}/menus")
    public ApiResponse<Void> replaceGroupMenus(@PathVariable Long id,
                                               @Valid @RequestBody MenuAssignRequest request) {
        menuService.replaceGroupMenus(id, request.menuIds());
        return ApiResponse.ok();
    }

    /** 사원이 속할 그룹을 통째로 교체한다. 비우면 기본 그룹이 적용된다. */
    @PutMapping("/employees/{id}/groups")
    public ApiResponse<Void> replaceEmployeeGroups(@PathVariable Long id,
                                                   @Valid @RequestBody GroupAssignRequest request) {
        menuService.replaceEmployeeGroups(id, request.groupIds());
        return ApiResponse.ok();
    }

    @GetMapping("/employees/{id}/groups")
    public ApiResponse<List<Long>> employeeGroups(@PathVariable Long id) {
        return ApiResponse.ok(menuService.findEmployeeGroupIds(id));
    }
}
