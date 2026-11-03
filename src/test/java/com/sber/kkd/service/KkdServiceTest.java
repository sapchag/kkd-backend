package com.sber.kkd.service;

import com.sber.kkd.api.dto.DeltaCountsDto;
import com.sber.kkd.api.dto.MetricDto;
import com.sber.kkd.api.dto.SummaryDto;
import com.sber.kkd.api.exception.ApiException;
import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.Issue;
import com.sber.kkd.domain.KkdRun;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.repository.GoalRepository;
import com.sber.kkd.repository.IssueRepository;
import com.sber.kkd.repository.RunRepository;
import com.sber.kkd.repository.UnitRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты расчёта метрик и дельты в KkdService (каскадирование, текущее/прошлое/дельта).
 */
class KkdServiceTest {

    private UnitRepository unitRepository = mock(UnitRepository.class);
    private GoalRepository goalRepository = mock(GoalRepository.class);
    private IssueRepository issueRepository = mock(IssueRepository.class);
    private RunRepository runRepository = mock(RunRepository.class);
    private RunService runService = mock(RunService.class);

    private KkdService service = new KkdService(
            unitRepository, goalRepository, issueRepository, runRepository, runService);

    private KkdRun run(long id, String code, Long comparedTo) {
        KkdRun r = new KkdRun();
        r.setId(id);
        r.setCode(code);
        r.setSnapshotDate(LocalDate.of(2026, 10, 26));
        r.setStatus(KkdRun.STATUS_SUCCESS);
        r.setComparedToRunId(comparedTo);
        return r;
    }

    private Unit unit(long id, String code, Long parentId) {
        Unit u = new Unit();
        u.setId(id);
        u.setCode(code);
        u.setParentId(parentId);
        return u;
    }

    private Goal active(long id, long unitId) {
        Goal g = new Goal();
        g.setId(id);
        g.setCode("G" + id);
        g.setUnitId(unitId);
        g.setStatus("ACTIVE");
        g.setOwner("Ivanov");
        return g;
    }

    private Issue issue(long runId, String rule, String objectRef, String severity) {
        Issue i = new Issue();
        i.setRunId(runId);
        i.setRuleCode(rule);
        i.setObjectRef(objectRef);
        i.setSeverity(severity);
        i.setUnitId(2L);
        return i;
    }

    /** Метрика и дельта считаются с учётом дочерних подразделений. */
    @Test
    void summaryComputesChildrenAndDelta() {
        Long runId = 2L, prevRunId = 1L;
        when(runService.getRequiredRun(runId)).thenReturn(run(runId, "RUN-002", prevRunId));
        when(runRepository.findById(prevRunId)).thenReturn(Optional.of(run(prevRunId, "RUN-001", null)));

        Unit hq = unit(1, "HQ-CIB", null);
        Unit child = unit(2, "CIB-SALES", 1L);
        when(unitRepository.findAll()).thenReturn(List.of(hq, child));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(hq));

        // дерево HQ-CIB = {1, 2}: две активные цели
        List<Goal> goals = List.of(active(10, 1L), active(20, 2L));

        // RUN-002: goal:10 грязная -> 1 проблема, критическая
        List<Issue> curIssues = List.of(
                issue(runId, "GOAL_NO_OWNER", "goal:10", "critical"),
                issue(runId, "CASCADE_NO_PARENT", "goal:20", "major")
        );
        // RUN-001: только goal:20
        List<Issue> prevIssues = List.of(
                issue(prevRunId, "CASCADE_NO_PARENT", "goal:20", "major")
        );

        when(goalRepository.findByUnitIdIn(anyCollection())).thenReturn(goals);
        when(issueRepository.findByRunId(runId)).thenReturn(curIssues);
        when(issueRepository.findByRunId(prevRunId)).thenReturn(prevIssues);

        SummaryDto summary = service.summary(runId, 1L);

        // RUN-002: 2 активные, 0 корректных (обе имеют проблемы) -> доля 0.0
        MetricDto share = summary.metrics().get(0);
        assertEquals("clean_goals_share", share.key());
        assertEquals(0.0, share.value());
        assertEquals(50.0, share.previous()); // RUN-001: только goal:20 грязная -> 1/2 корректных = 50%

        // fallback для correct count current: обе dirty
        MetricDto total = summary.metrics().get(1);
        assertEquals(2.0, total.value());
        assertEquals(1.0, total.previous());

        // дельта: RUN-002 current keys = {GOAL_NO_OWNER|goal:10, CASCADE|goal:20}
        // prev keys = {CASCADE|goal:20} -> persisting=1(goal:20), new=1(goal:10), resolved=0
        DeltaCountsDto delta = summary.delta();
        assertEquals(1, delta.newIssues());
        assertEquals(1, delta.persisting());
        assertEquals(0, delta.resolved());
    }

    /** Первый прогон без сравнения: previous и delta равны null. */
    @Test
    void firstRunHasNullPreviousAndDelta() {
        Long runId = 1L;
        when(runService.getRequiredRun(runId)).thenReturn(run(runId, "RUN-001", null));

        Unit hq = unit(1, "HQ-CIB", null);
        when(unitRepository.findAll()).thenReturn(List.of(hq));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(hq));

        List<Goal> goals = List.of(active(10, 1L)); // корректная, проблем нет
        when(goalRepository.findByUnitIdIn(anyCollection())).thenReturn(goals);
        when(issueRepository.findByRunId(runId)).thenReturn(List.of());

        SummaryDto summary = service.summary(runId, 1L);

        assertNull(summary.comparedTo());
        MetricDto share = summary.metrics().get(0);
        assertNull(share.previous());
        assertNull(share.delta());
        assertNull(share.trend());
    }

    /** Неуспешный прогон -> 409 RUN_FAILED. */
    @Test
    void failedRunThrows409() {
        KkdRun failed = new KkdRun();
        failed.setId(6L);
        failed.setCode("RUN-006");
        failed.setStatus(KkdRun.STATUS_FAILED);
        when(runService.getRequiredRun(6L)).thenThrow(ApiException.runFailed("Прогон завершился с ошибкой: RUN-006"));

        ApiException ex = assertThrows(ApiException.class, () -> service.summary(6L, 1L));
        assertEquals(409, ex.getStatus());
        assertEquals("RUN_FAILED", ex.getCode());
    }
}