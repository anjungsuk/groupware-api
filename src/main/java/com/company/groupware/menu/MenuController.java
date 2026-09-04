package com.company.groupware.menu;

import com.company.groupware.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 내 메뉴 — 화면이 이 결과만 그린다. 프론트는 스스로 권한을 판단하지 않는다. */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/my")
    public ApiResponse<List<MenuNode>> my(Authentication authentication) {
        return ApiResponse.ok(menuService.findMyMenus(Long.valueOf(authentication.getName())));
    }
}
