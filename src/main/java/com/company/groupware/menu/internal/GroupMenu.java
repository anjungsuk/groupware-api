package com.company.groupware.menu.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

/** 그룹 ↔ 메뉴 매핑. 조회 전용에 가까워 복합 키만 둔다. */
@Entity
@Table(name = "group_menus")
@IdClass(GroupMenu.Key.class)
public class GroupMenu {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Id
    @Column(name = "menu_id")
    private Long menuId;

    protected GroupMenu() {
    }

    public GroupMenu(Long groupId, Long menuId) {
        this.groupId = groupId;
        this.menuId = menuId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public record Key(Long groupId, Long menuId) implements Serializable {
        public Key() {
            this(null, null);
        }
    }
}
