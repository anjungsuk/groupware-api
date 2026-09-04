package com.company.groupware.menu.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeGroupRepository extends JpaRepository<EmployeeGroup, EmployeeGroup.Key> {

    List<EmployeeGroup> findByEmployeeId(Long employeeId);

    List<EmployeeGroup> findByGroupId(Long groupId);

    void deleteByEmployeeId(Long employeeId);
}
