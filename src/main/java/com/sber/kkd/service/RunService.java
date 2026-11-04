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

    /**
     * Возвращает успешный прогон по коду (например "RUN-002").
     * 404, если прогон не найден; 409 RUN_FAILED, если прогон завершился с ошибкой.
     */
    @Transactional(readOnly = true)
    public KkdRun getRequiredRun(String code) {
        KkdRun run = runRepository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Прогон не найден: " + code));
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
     * Находит успешный прогон по коду, если он существует и успешен; иначе null.
     */
    @Transactional(readOnly = true)
    public KkdRun findSuccessRunByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        KkdRun run = runRepository.findByCode(code).orElse(null);
        if (run == null || !KkdRun.STATUS_SUCCESS.equals(run.getStatus())) {
            return null;
        }
        return run;
    }
}