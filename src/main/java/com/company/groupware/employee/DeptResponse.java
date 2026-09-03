package com.company.groupware.employee;

/** 부서 선택지 — 프론트 `Dept` 와 1:1 (groupware-front · src/types/auth.ts) */
public record DeptResponse(Long id, String name, Long parentId) {

    public static DeptResponse from(Dept dept) {
        return new DeptResponse(dept.getId(), dept.getName(), dept.getParentId());
    }
}
