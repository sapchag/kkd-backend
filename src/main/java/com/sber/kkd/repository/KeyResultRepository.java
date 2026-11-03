package com.sber.kkd.repository;

import com.sber.kkd.domain.KeyResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface KeyResultRepository extends JpaRepository<KeyResult, Long> {

    List<KeyResult> findByGoalIdIn(Collection<Long> goalIds);

    List<KeyResult> findByGoalId(Long goalId);

    long countByGoalId(Long goalId);
}