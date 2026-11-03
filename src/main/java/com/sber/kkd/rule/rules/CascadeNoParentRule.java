package com.sber.kkd.rule.rules;

import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.Unit;
import com.sber.kkd.rule.KkdRule;
import com.sber.kkd.rule.RuleContext;
import com.sber.kkd.rule.RuleIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CASCADE_NO_PARENT (major): активная цель не связана с целью вышестоящего уровня.
 *
 * Каскадирование: каждая цель в дочернем (не корневом) подразделении должна быть
 * связана с целью вышестоящего уровня — либо напрямую через parent_goal_id, при котором
 * родительская цель находится в подразделении выше по иерархии unit, либо, если ветка
 * уже покрыта (родительская цепочка спускается вниз), сама является частью каскада.
 *
 * Упрощённая формализация: цель нарушает правило, если она не имеет parent_goal_id
 * И при этом её подразделение имеет родителя (т.е. это не корневое подразделение).
 * Цели в корневом подразделении (unit без parent) исключаются — им не на что каскадироваться.
 */
@Component
public class CascadeNoParentRule implements KkdRule {

    @Override
    public String code() {
        return "CASCADE_NO_PARENT";
    }

    @Override
    public List<RuleIssue> evaluate(RuleContext context) {
        Map<Long, Unit> unitById = new HashMap<>();
        for (Unit u : context.units()) {
            unitById.put(u.getId(), u);
        }

        List<RuleIssue> issues = new ArrayList<>();
        for (Goal goal : context.activeGoals()) {
            Unit unit = unitById.get(goal.getUnitId());
            if (goal.getParentGoalId() != null) {
                continue; // цель связана с родителем — считаем каскад выполненным
            }
            if (unit == null || unit.getParentId() == null) {
                continue; // корневое подразделение или неизвестное — не проверяем
            }
            issues.add(new RuleIssue(
                    code(),
                    "goal:" + goal.getId(),
                    goal.getUnitId(),
                    "major",
                    "нет",
                    "родительская цель вышестоящего уровня",
                    "Связать цель с целью вышестоящего уровня"
            ));
        }
        return issues;
    }
}