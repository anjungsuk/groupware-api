package com.company.groupware.menu;

import java.util.List;

/** 사원이 속한 그룹. 사원 권한 화면이 목록을 한 번에 받아 N+1 을 피한다. */
public record EmployeeGroupResponse(Long employeeId, List<Long> groupIds) {
}
