package com.company.groupware.approval;

import com.company.groupware.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 결재 양식 — TRD §3.1 DocForm.
 * 필드정의와 기본결재선은 양식마다 달라 JSON 으로 둔다 (TRD §3.2).
 */
@Entity
@Table(name = "doc_forms")
public class DocForm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_schema", nullable = false, columnDefinition = "jsonb")
    private String fieldSchema;

    /** 기본 결재선 — 직급 기반 단계 정의 (PRD §5). {@link DefaultLine} 로 해석한다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_line", nullable = false, columnDefinition = "jsonb")
    private String defaultLine;

    @Column(nullable = false)
    private boolean active;

    protected DocForm() {
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

    public int getVersion() {
        return version;
    }

    public String getFieldSchema() {
        return fieldSchema;
    }

    public String getDefaultLine() {
        return defaultLine;
    }

    public boolean isActive() {
        return active;
    }
}
