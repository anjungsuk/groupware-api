package com.company.groupware.menu;

import com.company.groupware.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 메뉴 — 노출 권한을 코드가 아니라 데이터로 관리한다.
 *
 * 2단이다. 상위(GNB 대분류)는 path 가 없고 하위(LNB 항목)가 path 를 갖는다.
 * path 는 프론트 라우트와 맞아야 하므로 관리 화면이 등록 가능한 경로를 목록으로 제한한다.
 */
@Entity
@Table(name = "menus")
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    /** 대분류는 갈 곳이 없어 비어 있다. */
    @Column(length = 200)
    private String path;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    protected Menu() {
    }

    public void update(String name, String path, Long parentId, int sortOrder, boolean active) {
        this.name = name;
        this.path = path;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.active = active;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Long getParentId() {
        return parentId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
