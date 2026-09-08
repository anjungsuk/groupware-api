package com.company.groupware.menu.internal;

import com.company.groupware.menu.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByActiveTrueOrderBySortOrderAsc();

    Optional<Menu> findByCode(String code);

    /**
     * 사원이 볼 수 있는 메뉴 — 속한 그룹들의 합집합.
     * 그룹이 하나도 없으면 기본 그룹(is_default)을 쓴다. 신규 가입자를 따로 배정하지
     * 않아도 최소 메뉴가 열리게 하려는 것이다.
     */
    @Query("""
            SELECT m FROM Menu m
            WHERE m.active = true
              AND m.id IN (
                SELECT gm.menuId FROM GroupMenu gm
                WHERE gm.groupId IN (
                  SELECT eg.groupId FROM EmployeeGroup eg WHERE eg.employeeId = :employeeId
                )
                   OR (
                     NOT EXISTS (SELECT 1 FROM EmployeeGroup e WHERE e.employeeId = :employeeId)
                     AND gm.groupId IN (SELECT g.id FROM PermissionGroup g WHERE g.isDefault = true)
                   )
              )
            ORDER BY m.sortOrder ASC
            """)
    List<Menu> findVisibleTo(@Param("employeeId") Long employeeId);
}
