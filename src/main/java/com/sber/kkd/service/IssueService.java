package com.sber.kkd.service;

import com.sber.kkd.api.dto.IssueDto;
import com.sber.kkd.api.dto.IssuesPageDto;
import com.sber.kkd.domain.Issue;
import com.sber.kkd.domain.KkdRun;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.repository.IssueRepository;
import com.sber.kkd.repository.RunRepository;
import com.sber.kkd.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис списка проблем: фильтры по правилу/важности/подразделению/статусу дельты,
 * пагинация, расчёт статуса дельта (new / persisting / resolved).
 */
@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final UnitRepository unitRepository;
    private final RunRepository runRepository;
    private final RunService runService;

    public IssueService(IssueRepository issueRepository,
                        UnitRepository unitRepository,
                        RunRepository runRepository,
                        RunService runService) {
        this.issueRepository = issueRepository;
        this.unitRepository = unitRepository;
        this.runRepository = runRepository;
        this.runService = runService;
    }

    @Transactional(readOnly = true)
    public IssuesPageDto issues(String runCode, IssuesRequest request, int limit, int offset) {
        KkdRun run = runService.getRequiredRun(runCode);

        List<Issue> all = issueRepository.findByRunId(run.getId());
        List<Issue> previous = List.of();
        if (run.getComparedToRunId() != null) {
            KkdRun cmp = runRepository.findById(run.getComparedToRunId()).orElse(null);
            if (cmp != null && KkdRun.STATUS_SUCCESS.equals(cmp.getStatus())) {
                previous = issueRepository.findByRunId(cmp.getId());
            }
        }
        Set<String> deltaKeys = deltaKeySet(previous);
        Map<Long, String> unitCodeById = unitRepository.findAll().stream()
                .collect(Collectors.toMap(Unit::getId, Unit::getCode));

        // Фильтры
        List<Issue> filtered = all.stream()
                .filter(iss -> request.ruleCode() == null || request.ruleCode().equals(iss.getRuleCode()))
                .filter(iss -> request.severity() == null || request.severity().equals(iss.getSeverity()))
                .filter(iss -> {
                    if (request.unitCode() == null) {
                        return true;
                    }
                    Long unitId = iss.getUnitId();
                    return unitId != null && request.unitCode().equals(unitCodeById.get(unitId));
                })
                .filter(iss -> {
                    if (request.deltaStatus() == null) {
                        return true;
                    }
                    return request.deltaStatus().equals(deltaStatus(deltaKeys, iss));
                })
                .toList();

        long total = filtered.size();
        long safeOffset = Math.max(0, offset);
        int from = (int) Math.min(safeOffset, total);
        int to = (int) Math.min(safeOffset + limit, total);
        List<Issue> page = filtered.subList(from, to);

        List<IssueDto> items = page.stream()
                .map(iss -> toDto(iss, unitCodeById, deltaKeys))
                .toList();

        return new IssuesPageDto(items, total, limit, offset);
    }

    private IssueDto toDto(Issue iss, Map<Long, String> unitCodeById, Set<String> deltaKeys) {
        return new IssueDto(
                iss.getId(),
                iss.getRuleCode(),
                iss.getSeverity(),
                iss.getObjectRef(),
                iss.getUnitId() == null ? null : unitCodeById.get(iss.getUnitId()),
                iss.getActualValue(),
                iss.getExpectedValue(),
                iss.getRecommendation(),
                deltaStatus(deltaKeys, iss)
        );
    }

    private String deltaStatus(Set<String> deltaKeys, Issue issue) {
        String key = issue.getRuleCode() + "|" + issue.getObjectRef();
        return deltaKeys.contains(key) ? "persisting" : "new";
    }

    private Set<String> deltaKeySet(List<Issue> previous) {
        return previous.stream()
                .map(iss -> iss.getRuleCode() + "|" + iss.getObjectRef())
                .collect(Collectors.toSet());
    }
}