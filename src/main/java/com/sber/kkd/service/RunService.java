package com.sber.kkd.service;

import com.sber.kkd.api.dto.RunDto;
import com.sber.kkd.api.exception.ApiException;
import com.sber.kkd.domain.Issue;
import com.sber.kkd.domain.KkdRun;
import com.sber.kkd.repository.IssueRepository;
import com.sber.kkd.repository.RunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RunService {

    private final RunRepository runRepository;
    private final IssueRepository issueRepository;

    public RunService(RunRepository runRepository, IssueRepository issueRepository) {
        this.runRepository = runRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public List<RunDto> listRuns() {
        return runRepository.findAllByOrderBySnapshotDateDesc().stream()
                .map(run -> {
                    KkdRun compared = run.getComparedToRunId() == null
                            ? null
                            : runRepository.findById(run.getComparedToRunId()).orElse(null);
                    long issueCount = issueRepository.findByRunId(run.getId()).size();
                    return new RunDto(
                            run.getCode(),
                            run.getSnapshotDate(),
                            run.getStatus(),
                            compared == null ? null : compared.getCode(),
                            issueCount
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public KkdRun getRequiredRun(Long runId) {
        KkdRun run = runRepository.findById(runId)
                .orElseThrow(() -> ApiException.notFound("Прогон не найден: " + runId));
        if (!KkdRun.STATUS_SUCCESS.equals(run.getStatus())) {
            throw ApiException.runFailed("Прогон завершился с ошибкой: " + run.getCode());
        }
        return run;
    }

    @Transactional(readOnly = true)
    public List<Issue> issuesOf(Long runId) {
        return issueRepository.findByRunId(runId);
    }

    /**
     * Находит успешный прогон по id, если он существует и успешен; иначе null.
     * Используется для сравнения: если прошлый прогон упал — сравнения нет.
     */
    @Transactional(readOnly = true)
    public KkdRun findSuccessRun(Long runId) {
        if (runId == null) {
            return null;
        }
        KkdRun run = runRepository.findById(runId).orElse(null);
        if (run == null || !KkdRun.STATUS_SUCCESS.equals(run.getStatus())) {
            return null;
        }
        return run;
    }
}