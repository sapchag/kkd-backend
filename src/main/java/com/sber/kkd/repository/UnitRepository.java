package com.sber.kkd.repository;

import com.sber.kkd.domain.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByCode(String code);
}