package com.company.groupware.employee.internal;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 사번 채번 — `EMP-{연도}-{4자리}`.
 * DB 시퀀스(employee_no_seq)를 쓰므로 동시 가입에도 번호가 중복되지 않는다.
 */
@Component
public class EmployeeNoGenerator {

    private static final String PREFIX = "EMP";

    private final EntityManager entityManager;

    public EmployeeNoGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public String next() {
        Number seq = (Number) entityManager
                .createNativeQuery("SELECT nextval('employee_no_seq')")
                .getSingleResult();

        return "%s-%d-%04d".formatted(PREFIX, LocalDate.now().getYear(), seq.longValue());
    }
}
