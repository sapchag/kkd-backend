package com.sber.kkd.repository;

import com.sber.kkd.domain.KkdRun;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RunRepository extends CrudRepository<KkdRun, Long> {

    List<KkdRun> findAllByOrderBySnapshotDateDesc();
}