package com.company.groupware.menu.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMenuRepository extends JpaRepository<GroupMenu, GroupMenu.Key> {

    List<GroupMenu> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);
}
