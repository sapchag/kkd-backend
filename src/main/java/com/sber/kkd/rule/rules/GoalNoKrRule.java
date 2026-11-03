package com.sber.kkd.rule.rules;

import com.sber.kkd.domain.Goal;
import com.sber.kkd.rule.KkdRule;
import com.sber.kkd.rule.RuleContext;
import com.sber.kkd.rule.RuleIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GOAL_NO_KR (critical): у активной цели нет ни одного ключевого результата.
 */
@Component
public class GoalNoKrRule implements KkdRule {

    @Override
    public String code() {
        return "GOAL_NO_KR";
    }

    @Override
    public List<RuleIssue> evaluate(RuleContext context) {
        Map<Long, List<com.sber.kkd.domain.KeyResult>> krs = context.krsGroupedByGoal();
        List<RuleIssue> issues = new ArrayList<>();
        for (Goal goal : context.activeGoals()) {
            List<com.sber.kkd.domain.KeyResult> goalKrs = krs.getOrDefault(goal.getId(), List.of());
            if (goalKrs.isEmpty()) {
                issues.add(new RuleIssue(
                        code(),
                        "goal:" + goal.getId(),
                        goal.getUnitId(),
                        "critical",
                        "0",
                        ">=1",
                        "Добавить хотя бы один ключевой результат"
                ));
            }
        }
        return issues;
    }
}