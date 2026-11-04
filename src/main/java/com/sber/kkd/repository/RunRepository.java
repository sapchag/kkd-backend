package com.sber.kkd.repository;

import com.sber.kkd.domain.KkdRun;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RunRepository extends CrudRepository<KkdRun, Long> {

    List<KkdRun> findAllByOrderBySnapshotDateDesc();

    Optional<KkdRun> findByCode(String code);
}