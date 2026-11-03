package com.sber.kkd.repository;

import com.sber.kkd.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByRunId(Long runId);

    List<Issue> findByRunIdIn(Collection<Long> runIds);
}