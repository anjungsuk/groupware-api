package com.company.groupware.menu.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

/** 사원 ↔ 권한 그룹 매핑. */
@Entity
@Table(name = "employee_groups")
@IdClass(EmployeeGroup.Key.class)
public class EmployeeGroup {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @Id
    @Column(name = "group_id")
    private Long groupId;

    protected EmployeeGroup() {
    }

    public EmployeeGroup(Long employeeId, Long groupId) {
        this.employeeId = employeeId;
        this.groupId = groupId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public record Key(Long employeeId, Long groupId) implements Serializable {
        public Key() {
            this(null, null);
        }
    }
}
