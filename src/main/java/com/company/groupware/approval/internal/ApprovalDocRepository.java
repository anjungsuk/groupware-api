package com.company.groupware.approval.internal;

import com.company.groupware.approval.ApprovalDoc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDocRepository extends JpaRepository<ApprovalDoc, Long> {
}
