package com.sber.kkd.repository;

import com.sber.kkd.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUnitIdIn(Collection<Long> unitIds);

    List<Goal> findByStatusEquals(String status);
}