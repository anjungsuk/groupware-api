package com.company.groupware.approval.internal;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 문서번호 채번 — `SH-0000001` (TRD §4.3).
 * DB 시퀀스를 쓰므로 동시 상신에도 번호가 겹치지 않는다. 사번 채번과 같은 방식이다.
 */
@Component
@RequiredArgsConstructor
public class DocNoGenerator {

    private static final String PREFIX = "SH";

    private final EntityManager entityManager;

    public String next() {
        Number seq = (Number) entityManager
                .createNativeQuery("SELECT nextval('doc_no_seq')")
                .getSingleResult();

        return "%s-%07d".formatted(PREFIX, seq.longValue());
    }
}
