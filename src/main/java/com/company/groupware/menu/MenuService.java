package com.company.groupware.menu;

import com.company.groupware.common.exception.BusinessException;
import com.company.groupware.common.exception.ErrorCode;
import com.company.groupware.common.exception.FieldValidationException;
import com.company.groupware.menu.internal.EmployeeGroup;
import com.company.groupware.menu.internal.EmployeeGroupRepository;
import com.company.groupware.menu.internal.GroupMenu;
import com.company.groupware.menu.internal.GroupMenuRepository;
import com.company.groupware.menu.internal.MenuRepository;
import com.company.groupware.menu.internal.PermissionGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 메뉴 · 권한 그룹 — 메뉴 모듈의 공식 API.
 *
 * 메뉴 노출은 사원 → 그룹 → 메뉴 로 결정된다. 프론트는 이 결과만 그리고
 * 스스로 권한을 판단하지 않는다 — 판단이 두 곳에 있으면 반드시 어긋난다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final PermissionGroupRepository groupRepository;
    private final GroupMenuRepository groupMenuRepository;
    private final EmployeeGroupRepository employeeGroupRepository;

    /** 내가 볼 수 있는 메뉴 트리. */
    public List<MenuNode> findMyMenus(Long employeeId) {
        return toTree(menuRepository.findVisibleTo(employeeId));
    }

    /** 메뉴 등록. code 는 유일해야 한다 — 권한 매핑과 시드가 code 로 걸려 있다. */
    @Transactional
    public Menu createMenu(MenuSaveRequest request) {
        menuRepository.findByCode(request.code()).ifPresent(existing -> {
            throw FieldValidationException.of("code", "이미 쓰고 있는 코드입니다.");
        });
        validateParent(request.parentId(), null);

        return menuRepository.save(Menu.create(request.code(), request.name(),
                blankToNull(request.path()), request.parentId(), request.sortOrder()));
    }

    /** 메뉴 수정. code 는 바꾸지 않는다 — 바뀌면 권한 매핑이 조용히 끊긴다. */
    @Transactional
    public Menu updateMenu(Long menuId, MenuSaveRequest request) {
        Menu menu = findMenu(menuId);
        validateParent(request.parentId(), menuId);

        menu.update(request.name(), blankToNull(request.path()), request.parentId(),
                request.sortOrder(), request.active());
        return menu;
    }

    /** 메뉴 삭제. 하위 메뉴가 남아 있으면 거부한다 — 갈 곳 없는 항목이 생긴다. */
    @Transactional
    public void deleteMenu(Long menuId) {
        Menu menu = findMenu(menuId);

        boolean hasChildren = menuRepository.findAll().stream()
                .anyMatch(child -> menuId.equals(child.getParentId()));
        if (hasChildren) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "하위 메뉴가 있어 삭제할 수 없습니다. 하위 메뉴를 먼저 정리해 주세요.");
        }

        // 권한 매핑도 함께 지운다 (FK ON DELETE CASCADE 가 있지만 명시한다)
        groupMenuRepository.deleteByMenuId(menuId);
        menuRepository.delete(menu);
    }

    /** 사원별 그룹 — 사원 권한 화면이 한 번에 받아 N+1 을 피한다. */
    public List<EmployeeGroupResponse> findAllEmployeeGroups() {
        return employeeGroupRepository.findAll().stream()
                .collect(Collectors.groupingBy(EmployeeGroup::getEmployeeId,
                        Collectors.mapping(EmployeeGroup::getGroupId, Collectors.toList())))
                .entrySet().stream()
                .map(entry -> new EmployeeGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Menu findMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "메뉴를 찾을 수 없습니다."));
    }

    /** 상위는 최상위 메뉴만 될 수 있다. 2단을 넘기면 화면이 그리지 못한다. */
    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw FieldValidationException.of("parentId", "자기 자신을 상위로 둘 수 없습니다.");
        }

        Menu parent = menuRepository.findById(parentId)
                .orElseThrow(() -> FieldValidationException.of("parentId", "존재하지 않는 상위 메뉴입니다."));
        if (!parent.isRoot()) {
            throw FieldValidationException.of("parentId", "메뉴는 2단까지만 만들 수 있습니다.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** 관리 화면용 전체 메뉴(비활성 포함). */
    public List<Menu> findAllMenus() {
        return menuRepository.findAll(Sort.by("sortOrder"));
    }

    public List<PermissionGroupResponse> findGroups() {
        Map<Long, Long> memberCounts = employeeGroupRepository.findAll().stream()
                .collect(Collectors.groupingBy(EmployeeGroup::getGroupId, Collectors.counting()));

        return groupRepository.findAllByOrderByIdAsc().stream()
                .map(group -> new PermissionGroupResponse(
                        group.getId(), group.getCode(), group.getName(), group.getDescription(),
                        group.isDefault(),
                        groupMenuRepository.findByGroupId(group.getId()).stream()
                                .map(GroupMenu::getMenuId).sorted().toList(),
                        memberCounts.getOrDefault(group.getId(), 0L).intValue()))
                .toList();
    }

    /** 그룹에 열어 줄 메뉴를 통째로 교체한다. */
    @Transactional
    public void replaceGroupMenus(Long groupId, List<Long> menuIds) {
        PermissionGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "권한 그룹을 찾을 수 없습니다."));

        groupMenuRepository.deleteByGroupId(group.getId());
        groupMenuRepository.saveAll(menuIds.stream()
                .distinct()
                .map(menuId -> new GroupMenu(group.getId(), menuId))
                .toList());
    }

    /** 사원이 속할 그룹을 통째로 교체한다. 비우면 기본 그룹이 적용된다. */
    @Transactional
    public void replaceEmployeeGroups(Long employeeId, List<Long> groupIds) {
        employeeGroupRepository.deleteByEmployeeId(employeeId);
        employeeGroupRepository.saveAll(groupIds.stream()
                .distinct()
                .map(groupId -> new EmployeeGroup(employeeId, groupId))
                .toList());
    }

    public List<Long> findEmployeeGroupIds(Long employeeId) {
        return employeeGroupRepository.findByEmployeeId(employeeId).stream()
                .map(EmployeeGroup::getGroupId)
                .toList();
    }

    /** 평면 목록을 2단 트리로 접는다. 부모가 안 보이면 자식도 내려보내지 않는다. */
    private List<MenuNode> toTree(List<Menu> menus) {
        Map<Long, List<Menu>> byParent = menus.stream()
                .filter(menu -> !menu.isRoot())
                .collect(Collectors.groupingBy(Menu::getParentId));

        return menus.stream()
                .filter(Menu::isRoot)
                .sorted(Comparator.comparingInt(Menu::getSortOrder))
                .map(root -> new MenuNode(root.getId(), root.getCode(), root.getName(), root.getPath(),
                        byParent.getOrDefault(root.getId(), List.of()).stream()
                                .sorted(Comparator.comparingInt(Menu::getSortOrder))
                                .map(child -> new MenuNode(child.getId(), child.getCode(),
                                        child.getName(), child.getPath(), List.of()))
                                .toList()))
                .toList();
    }
}
