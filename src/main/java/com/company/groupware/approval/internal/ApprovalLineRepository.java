package com.company.groupware.approval.internal;

import com.company.groupware.approval.ApprovalLine;
import com.company.groupware.approval.LineResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

    List<ApprovalLine> findByDocIdOrderByStepAsc(Long docId);

    /** 결재 대기함 — 내 차례로 넘어온 라인. 문서 상태까지는 여기서 보지 않는다. */
    List<ApprovalLine> findByApproverIdAndResultOrderByStepAsc(Long approverId, LineResult result);
}
