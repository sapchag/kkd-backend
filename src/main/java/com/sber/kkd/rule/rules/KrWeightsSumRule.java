package com.sber.kkd.rule.rules;

import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.KeyResult;
import com.sber.kkd.rule.KkdRule;
import com.sber.kkd.rule.RuleContext;
import com.sber.kkd.rule.RuleIssue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KR_WEIGHTS_SUM (major): сумма весов ключевых результатов активной цели
 * не равна 100%. Веса хранятся в процентах (50 = 50%).
 */
@Component
public class KrWeightsSumRule implements KkdRule {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public String code() {
        return "KR_WEIGHTS_SUM";
    }

    @Override
    public List<RuleIssue> evaluate(RuleContext context) {
        Map<Long, List<KeyResult>> krs = context.krsGroupedByGoal();
        List<RuleIssue> issues = new ArrayList<>();

        for (Goal goal : context.activeGoals()) {
            List<KeyResult> goalKrs = krs.getOrDefault(goal.getId(), List.of());
            if (goalKrs.isEmpty()) {
                continue; // случай отсутствия КР обрабатывает GOAL_NO_KR
            }
            BigDecimal sum = goalKrs.stream()
                    .map(KeyResult::getWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(HUNDRED) != 0) {
                issues.add(new RuleIssue(
                        code(),
                        "goal:" + goal.getId(),
                        goal.getUnitId(),
                        "major",
                        sum.stripTrailingZeros().toPlainString() + "%",
                        "100%",
                        "Скорректировать веса ключевых результатов, чтобы их сумма была равна 100%"
                ));
            }
        }
        return issues;
    }
}