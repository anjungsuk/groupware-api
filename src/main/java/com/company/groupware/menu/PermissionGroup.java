package com.company.groupware.menu;

import com.company.groupware.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 권한 그룹 — 사원과 메뉴 사이의 단위.
 *
 * isDefault 는 아무 그룹에도 속하지 않은 사원이 받는 그룹이다.
 * 신규 가입자를 따로 배정하지 않아도 최소 메뉴가 열린다.
 */
@Entity
@Table(name = "permission_groups")
public class PermissionGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    protected PermissionGroup() {
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
