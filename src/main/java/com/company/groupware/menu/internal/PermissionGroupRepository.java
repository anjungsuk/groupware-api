package com.company.groupware.menu.internal;

import com.company.groupware.menu.PermissionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionGroupRepository extends JpaRepository<PermissionGroup, Long> {

    List<PermissionGroup> findAllByOrderByIdAsc();
}
