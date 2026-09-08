package com.company.groupware.approval.internal;

import com.company.groupware.approval.ApprovalDoc;
import com.company.groupware.approval.DocStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApprovalDocRepository extends JpaRepository<ApprovalDoc, Long> {

    /** 상신함·임시저장함·완료함 — 내가 올린 문서를 상태로 거른다. */
    List<ApprovalDoc> findByDrafterIdAndStatusInOrderByIdDesc(
            Long drafterId, Collection<DocStatus> statuses);
}
