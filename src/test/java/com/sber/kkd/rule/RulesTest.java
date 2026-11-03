package com.sber.kkd.rule;

import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.KeyResult;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.rule.rules.GoalNoOwnerRule;
import com.sber.kkd.rule.rules.CascadeNoParentRule;
import com.sber.kkd.rule.rules.KrWeightsSumRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesTest {

    private Goal active(long id, Long parentGoalId, long unitId, String owner) {
        Goal g = new Goal();
        g.setId(id);
        g.setCode("G" + id);
        g.setUnitId(unitId);
        g.setParentGoalId(parentGoalId);
        g.setStatus("ACTIVE");
        g.setOwner(owner);
        return g;
    }

    private Unit unit(long id, Long parentId) {
        Unit u = new Unit();
        u.setId(id);
        u.setCode("U" + id);
        u.setParentId(parentId);
        return u;
    }

    /** GOAL_NO_OWNER: цель с владельцем не нарушает, без владельца — нарушает. */
    @Test
    void goalNoOwnerFindsOnlyGoalsWithoutOwner() {
        List<Goal> goals = List.of(
                active(1, null, 1, "Ivanov"),
                active(2, null, 1, null),
                active(3, null, 1, "  ")
        );
        RuleContext ctx = new RuleContext(List.of(unit(1, null)), goals, List.of(), 10L);

        List<RuleIssue> issues = new GoalNoOwnerRule().evaluate(ctx);

        assertEquals(2, issues.size());
        assertTrue(issues.stream().allMatch(i -> i.ruleCode().equals("GOAL_NO_OWNER")));
        assertTrue(issues.stream().anyMatch(i -> i.objectRef().equals("goal:2")));
        assertTrue(issues.stream().anyMatch(i -> i.objectRef().equals("goal:3")));
    }

    /** KR_WEIGHTS_SUM: сумма 100 — ок; 90 — нарушение; цель без КР не нарушает это правило. */
    @Test
    void krWeightsSumBoundary() {
        Goal gOk = active(1, null, 1, "Ivanov");
        Goal gBad = active(2, null, 1, "Petrov");
        Goal gNoKr = active(3, null, 1, "Sidorov");

        KeyResult kr1 = kr(gOk.getId(), "50");
        KeyResult kr2 = kr(gOk.getId(), "50"); // 100 — ок
        KeyResult kr3 = kr(gBad.getId(), "60");
        KeyResult kr4 = kr(gBad.getId(), "30"); // 90 — нарушение

        RuleContext ctx = new RuleContext(
                List.of(unit(1, null)),
                List.of(gOk, gBad, gNoKr),
                List.of(kr1, kr2, kr3, kr4),
                10L
        );

        List<RuleIssue> issues = new KrWeightsSumRule().evaluate(ctx);

        assertEquals(1, issues.size());
        assertEquals("goal:2", issues.get(0).objectRef());
        assertEquals("KR_WEIGHTS_SUM", issues.get(0).ruleCode());
    }

    /** CASCADE_NO_PARENT: цель в корневом подразделении не нарушает; в дочернем без родителя — нарушает. */
    @Test
    void cascadeRootExcludedChildViolates() {
        Goal rootGoal = active(1, null, 1, "Ivanov");     // unit 1 — корневой
        Goal childGoal = active(2, null, 2, "Petrov");    // unit 2 — дочерний, без родителя
        Goal linkedChild = active(3, 1L, 2, "Sidorov");   // unit 2 — с родителем

        RuleContext ctx = new RuleContext(
                List.of(unit(1, null), unit(2, 1L)),
                List.of(rootGoal, childGoal, linkedChild),
                List.of(),
                10L
        );

        List<RuleIssue> issues = new CascadeNoParentRule().evaluate(ctx);

        assertEquals(1, issues.size());
        assertEquals("goal:2", issues.get(0).objectRef());
    }

    private KeyResult kr(Long goalId, String weight) {
        KeyResult kr = new KeyResult();
        kr.setGoalId(goalId);
        kr.setWeight(new BigDecimal(weight));
        return kr;
    }
}