package com.company.groupware.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 직급 마스터. PRD §5 결재선상 차장=1차 승인자, 실장=최종 승인자. */
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Position() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
