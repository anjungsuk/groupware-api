package com.company.groupware.menu;

import com.company.groupware.menu.internal.EmployeeGroup;
import com.company.groupware.menu.internal.EmployeeGroupRepository;
import com.company.groupware.menu.internal.GroupMenu;
import com.company.groupware.menu.internal.GroupMenuRepository;
import com.company.groupware.menu.internal.MenuRepository;
import com.company.groupware.menu.internal.PermissionGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * 메뉴 노출 — 사원 → 그룹 → 메뉴.
 *
 * 프론트는 이 결과만 그리므로 여기가 틀리면 메뉴가 새거나 사라진다.
 * 특히 부모가 안 보이는데 자식만 내려가면 화면에 갈 곳 없는 항목이 뜬다.
 */
class MenuServiceTest {

    private MenuRepository menuRepository;
    private PermissionGroupRepository groupRepository;
    private GroupMenuRepository groupMenuRepository;
    private EmployeeGroupRepository employeeGroupRepository;
    private MenuService service;

    @BeforeEach
    void setUp() {
        menuRepository = mock(MenuRepository.class);
        groupRepository = mock(PermissionGroupRepository.class);
        groupMenuRepository = mock(GroupMenuRepository.class);
        employeeGroupRepository = mock(EmployeeGroupRepository.class);
        service = new MenuService(menuRepository, groupRepository, groupMenuRepository,
                employeeGroupRepository);
    }

    /** Menu 는 생성 경로가 없다 — 관리 화면(등록)이 생기면 그 팩터리로 바꾼다. */
    private static Menu menu(long id, String code, String name, String path, Long parentId, int sort) {
        try {
            Constructor<Menu> constructor = Menu.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Menu menu = constructor.newInstance();
            set(menu, "id", id);
            set(menu, "code", code);
            set(menu, "name", name);
            set(menu, "path", path);
            set(menu, "parentId", parentId);
            set(menu, "sortOrder", sort);
            set(menu, "active", true);
            return menu;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    @DisplayName("상위 메뉴 아래에 하위 메뉴를 접어서 내려준다")
    void buildsTree() {
        given(menuRepository.findVisibleTo(1L)).willReturn(List.of(
                menu(1, "HOME", "홈", "/", null, 1),
                menu(2, "APPROVAL", "전자결재", null, null, 2),
                menu(3, "APPROVAL_NEW", "결재 작성", "/docs/new", 2L, 1),
                menu(4, "APPROVAL_SENT", "상신함", "/docs/sent", 2L, 2)));

        List<MenuNode> tree = service.findMyMenus(1L);

        assertThat(tree).extracting(MenuNode::code).containsExactly("HOME", "APPROVAL");
        assertThat(tree.get(0).children()).isEmpty();
        assertThat(tree.get(1).children()).extracting(MenuNode::name)
                .containsExactly("결재 작성", "상신함");
    }

    @Test
    @DisplayName("정렬 순서를 지킨다 — 조회 순서에 기대지 않는다")
    void sortsBySortOrder() {
        given(menuRepository.findVisibleTo(1L)).willReturn(List.of(
                menu(2, "APPROVAL", "전자결재", null, null, 2),
                menu(1, "HOME", "홈", "/", null, 1),
                menu(4, "APPROVAL_SENT", "상신함", "/docs/sent", 2L, 2),
                menu(3, "APPROVAL_NEW", "결재 작성", "/docs/new", 2L, 1)));

        List<MenuNode> tree = service.findMyMenus(1L);

        assertThat(tree).extracting(MenuNode::code).containsExactly("HOME", "APPROVAL");
        assertThat(tree.get(1).children()).extracting(MenuNode::code)
                .containsExactly("APPROVAL_NEW", "APPROVAL_SENT");
    }

    @Test
    @DisplayName("부모가 안 보이면 자식도 내려보내지 않는다 — 갈 곳 없는 항목이 뜨면 안 된다")
    void orphanChildIsDropped() {
        given(menuRepository.findVisibleTo(1L)).willReturn(List.of(
                menu(1, "HOME", "홈", "/", null, 1),
                // 관리 대분류(id 9)는 권한이 없어 빠졌는데 자식만 남은 상황
                menu(10, "ADMIN_MENU", "메뉴 관리", "/admin/menus", 9L, 1)));

        List<MenuNode> tree = service.findMyMenus(1L);

        assertThat(tree).extracting(MenuNode::code).containsExactly("HOME");
    }

    @Test
    @DisplayName("권한 없는 메뉴는 아예 내려오지 않는다")
    void hidesUnpermittedMenus() {
        given(menuRepository.findVisibleTo(1L)).willReturn(List.of(
                menu(1, "HOME", "홈", "/", null, 1)));

        List<MenuNode> tree = service.findMyMenus(1L);

        assertThat(tree).extracting(MenuNode::code).doesNotContain("ADMIN");
    }

    @Test
    @DisplayName("그룹 메뉴는 통째로 교체한다 — 지운 뒤 다시 넣는다")
    void replaceGroupMenusIsFullReplacement() {
        PermissionGroup group = mock(PermissionGroup.class);
        given(group.getId()).willReturn(7L);
        given(groupRepository.findById(7L)).willReturn(Optional.of(group));

        service.replaceGroupMenus(7L, List.of(1L, 2L, 2L));

        then(groupMenuRepository).should().deleteByGroupId(7L);

        ArgumentCaptor<List<GroupMenu>> saved = ArgumentCaptor.captor();
        then(groupMenuRepository).should().saveAll(saved.capture());
        // 중복은 걸러진다
        assertThat(saved.getValue()).extracting(GroupMenu::getMenuId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("사원 그룹을 비우면 기본 그룹이 적용되도록 아무것도 저장하지 않는다")
    void emptyGroupsFallBackToDefault() {
        service.replaceEmployeeGroups(5L, List.of());

        then(employeeGroupRepository).should().deleteByEmployeeId(5L);

        ArgumentCaptor<List<EmployeeGroup>> saved = ArgumentCaptor.captor();
        then(employeeGroupRepository).should().saveAll(saved.capture());
        assertThat(saved.getValue()).isEmpty();
    }

    @Test
    @DisplayName("그룹 목록에 소속 인원 수와 열린 메뉴를 함께 담는다")
    void groupsCarryMemberCountAndMenus() {
        PermissionGroup group = mock(PermissionGroup.class);
        given(group.getId()).willReturn(1L);
        given(group.getCode()).willReturn("MEMBER");
        given(group.getName()).willReturn("일반 사원");
        given(groupRepository.findAllByOrderByIdAsc()).willReturn(List.of(group));
        given(groupMenuRepository.findByGroupId(1L))
                .willReturn(List.of(new GroupMenu(1L, 3L), new GroupMenu(1L, 2L)));
        given(employeeGroupRepository.findAll())
                .willReturn(List.of(new EmployeeGroup(10L, 1L), new EmployeeGroup(11L, 1L)));

        List<PermissionGroupResponse> groups = service.findGroups();

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).menuIds()).containsExactly(2L, 3L);
        assertThat(groups.get(0).memberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("메뉴를 못 찾는 그룹에 배정하면 거부한다")
    void unknownGroupIsRejected() {
        given(groupRepository.findById(99L)).willReturn(Optional.empty());

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.replaceGroupMenus(99L, List.of(1L)))
                .hasMessage("권한 그룹을 찾을 수 없습니다.");

        then(groupMenuRepository).should(org.mockito.Mockito.never()).saveAll(anyList());
    }
}
