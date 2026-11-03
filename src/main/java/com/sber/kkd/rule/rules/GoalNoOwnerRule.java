package com.sber.kkd.rule.rules;

import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.rule.KkdRule;
import com.sber.kkd.rule.RuleContext;
import com.sber.kkd.rule.RuleIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GOAL_NO_OWNER (critical): активная цель не имеет ответственного (owner).
 */
@Component
public class GoalNoOwnerRule implements KkdRule {

    @Override
    public String code() {
        return "GOAL_NO_OWNER";
    }

    @Override
    public List<RuleIssue> evaluate(RuleContext context) {
        List<Goal> activeGoals = context.activeGoals();
        Map<Long, Unit> unitById = context.units().stream()
                .collect(Collectors.toMap(Unit::getId, u -> u));

        List<RuleIssue> issues = new ArrayList<>();
        for (Goal goal : activeGoals) {
            if (goal.getOwner() == null || goal.getOwner().isBlank()) {
                issues.add(new RuleIssue(
                        code(),
                        "goal:" + goal.getId(),
                        goal.getUnitId(),
                        "critical",
                        "пусто",
                        "ФИО ответственного",
                        "Назначить ответственного за цель"
                ));
            }
        }
        return issues;
    }
}