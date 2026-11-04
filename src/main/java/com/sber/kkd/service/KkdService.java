package com.sber.kkd.service;

import com.sber.kkd.api.dto.DeltaCountsDto;
import com.sber.kkd.api.dto.MetricDto;
import com.sber.kkd.api.dto.RunRefDto;
import com.sber.kkd.api.dto.SeverityCountDto;
import com.sber.kkd.api.dto.SummaryDto;
import com.sber.kkd.api.dto.UnitRefDto;
import com.sber.kkd.api.exception.ApiException;
import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.Issue;
import com.sber.kkd.domain.KkdRun;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.repository.GoalRepository;
import com.sber.kkd.repository.IssueRepository;
import com.sber.kkd.repository.RunRepository;
import com.sber.kkd.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис метрик ККД: считает метрики по подразделению с учётом дочерних
 * подразделений, дельту с прошлым прогоном и счётчики новых/сохраняющихся/исправленных.
 */
@Service
public class KkdService {

    private final UnitRepository unitRepository;
    private final GoalRepository goalRepository;
    private final IssueRepository issueRepository;
    private final RunRepository runRepository;
    private final RunService runService;

    public KkdService(UnitRepository unitRepository,
                      GoalRepository goalRepository,
                      IssueRepository issueRepository,
                      RunRepository runRepository,
                      RunService runService) {
        this.unitRepository = unitRepository;
        this.goalRepository = goalRepository;
        this.issueRepository = issueRepository;
        this.runRepository = runRepository;
        this.runService = runService;
    }

    /** Вход для расчёта одной метрики по срезу прогона. */
    private record MetricInput(long activeGoals, long cleanGoals, long issues, long criticalIssues) {
    }

    @Transactional(readOnly = true)
    public SummaryDto summary(String runCode, String unitCode) {
        KkdRun run = runService.getRequiredRun(runCode);
        Unit unit = unitRepository.findByCode(unitCode)
                .orElseThrow(() -> ApiException.notFound("Подразделение не найдено: " + unitCode));

        List<Unit> allUnits = unitRepository.findAll();
        Set<Long> treeIds = descendantsOf(unit, allUnits);

        KkdRun comparedRun = null;
        if (run.getComparedToRunId() != null) {
            KkdRun cmp = runRepository.findById(run.getComparedToRunId()).orElse(null);
            if (cmp != null && KkdRun.STATUS_SUCCESS.equals(cmp.getStatus())) {
                comparedRun = cmp;
            }
        }

        List<Goal> treeGoals = goalRepository.findByUnitIdIn(treeIds);
        List<Issue> currentIssues = issueRepository.findByRunId(run.getId());
        List<Issue> previousIssues = comparedRun == null ? List.of() : issueRepository.findByRunId(comparedRun.getId());

        MetricInput current = computeInput(treeGoals, currentIssues);
        MetricInput previous = comparedRun == null
                ? new MetricInput(0, 0, 0, 0)
                : computeInput(treeGoals, previousIssues);

        List<MetricDto> metrics = buildMetrics(current, previous, comparedRun != null);
        DeltaCountsDto delta = comparedRun == null
                ? new DeltaCountsDto(0, 0, 0)
                : computeDelta(currentIssues, previousIssues);
        List<SeverityCountDto> severities = buildSeverityCounts(currentIssues, previousIssues, comparedRun != null);

        return new SummaryDto(
                new RunRefDto(run.getCode(), run.getSnapshotDate()),
                comparedRun == null ? null : new RunRefDto(comparedRun.getCode(), comparedRun.getSnapshotDate()),
                new UnitRefDto(unit.getCode(), unit.getName()),
                metrics,
                severities,
                delta
        );
    }

    private MetricInput computeInput(List<Goal> goals, List<Issue> issues) {
        List<Issue> safe = issues == null ? List.of() : issues;
        // Цели, у которых есть хотя бы одна проблема с object_ref=goal:<id>.
        Set<String> dirtyGoalRefs = safe.stream()
                .filter(iss -> iss.getObjectRef().startsWith("goal:"))
                .map(Issue::getObjectRef)
                .collect(Collectors.toSet());

        long active = 0, clean = 0;
        for (Goal g : goals) {
            if (g.isActive()) {
                active++;
                if (!dirtyGoalRefs.contains("goal:" + g.getId())) {
                    clean++;
                }
            }
        }
        long critical = safe.stream().filter(iss -> "critical".equals(iss.getSeverity())).count();
        return new MetricInput(active, clean, safe.size(), critical);
    }

    private List<MetricDto> buildMetrics(MetricInput current, MetricInput previous, boolean hasComparison) {
        double currentShare = current.activeGoals() == 0
                ? 0.0
                : 100.0 * current.cleanGoals() / current.activeGoals();
        double previousShare = previous.activeGoals() == 0
                ? 0.0
                : 100.0 * previous.cleanGoals() / previous.activeGoals();

        List<MetricDto> result = new ArrayList<>();
        result.add(metric("clean_goals_share", "Доля корректных целей", "%",
                currentShare, previousShare, true, hasComparison));
        result.add(metric("issues_total", "Найдено проблем", "шт.",
                (double) current.issues(), (double) previous.issues(), false, hasComparison));
        result.add(metric("critical_issues", "Критические проблемы", "шт.",
                (double) current.criticalIssues(), (double) previous.criticalIssues(), false, hasComparison));
        return result;
    }

    private MetricDto metric(String key, String title, String unit,
                             Double value, Double previous, boolean higherIsBetter, boolean hasComparison) {
        Double delta = hasComparison ? round1(value - previous) : null;
        String trend = null;
        if (hasComparison) {
            double d = value - previous;
            trend = Math.abs(d) < 0.05 ? "same" : (d > 0) == higherIsBetter ? "better" : "worse";
        }
        return new MetricDto(
                key, title, unit,
                round1(value),
                hasComparison ? round1(previous) : null,
                delta, trend, higherIsBetter
        );
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private DeltaCountsDto computeDelta(List<Issue> current, List<Issue> previous) {
        if (previous == null || previous.isEmpty()) {
            return new DeltaCountsDto(current == null ? 0 : current.size(), 0, 0);
        }
        List<Issue> cur = current == null ? List.of() : current;
        Set<String> prevKeys = previous.stream().map(this::issueKey).collect(Collectors.toSet());
        Set<String> curKeys = cur.stream().map(this::issueKey).collect(Collectors.toSet());

        long persisting = curKeys.stream().filter(prevKeys::contains).count();
        long newIssues = curKeys.size() - persisting;
        long resolved = prevKeys.size() - persisting;
        return new DeltaCountsDto(newIssues, persisting, resolved);
    }

    private List<SeverityCountDto> buildSeverityCounts(List<Issue> current, List<Issue> previous, boolean hasComparison) {
        List<Issue> cur = current == null ? List.of() : current;
        List<Issue> prev = previous == null ? List.of() : previous;

        Set<String> severities = new HashSet<>(cur.stream().map(Issue::getSeverity).toList());
        severities.addAll(prev.stream().map(Issue::getSeverity).toList());

        List<SeverityCountDto> result = new ArrayList<>();
        for (String s : List.of("critical", "major", "minor")) {
            if (!severities.contains(s)) {
                continue;
            }
            long c = cur.stream().filter(i -> s.equals(i.getSeverity())).count();
            long p = prev.stream().filter(i -> s.equals(i.getSeverity())).count();
            result.add(new SeverityCountDto(s, c, hasComparison ? p : null, hasComparison ? c - p : null));
        }
        return result;
    }

    private String issueKey(Issue issue) {
        return issue.getRuleCode() + "|" + issue.getObjectRef();
    }

    /**
     * Возвращает множество id подразделений: сам узел + все его потомки (каскадирование).
     */
    private Set<Long> descendantsOf(Unit root, List<Unit> allUnits) {
        Map<Long, List<Unit>> childrenByParent = allUnits.stream()
                .collect(Collectors.groupingBy(u -> u.getParentId() == null ? -1L : u.getParentId()));
        Set<Long> result = new HashSet<>();
        result.add(root.getId());
        collectChildren(root.getId(), childrenByParent, result);
        return result;
    }

    private void collectChildren(Long parentId, Map<Long, List<Unit>> childrenByParent, Set<Long> acc) {
        List<Unit> children = childrenByParent.getOrDefault(parentId, List.of());
        for (Unit child : children) {
            acc.add(child.getId());
            collectChildren(child.getId(), childrenByParent, acc);
        }
    }
}