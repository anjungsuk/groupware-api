package com.company.groupware.employee.internal;

import com.company.groupware.employee.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, String> {

    List<Position> findAllByOrderBySortOrderAsc();
}
