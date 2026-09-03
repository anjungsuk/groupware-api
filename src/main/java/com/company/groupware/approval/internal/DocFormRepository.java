package com.company.groupware.approval.internal;

import com.company.groupware.approval.DocForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocFormRepository extends JpaRepository<DocForm, Long> {

    /** 같은 코드의 최신 버전 활성 양식 */
    Optional<DocForm> findFirstByCodeAndActiveTrueOrderByVersionDesc(String code);
}
