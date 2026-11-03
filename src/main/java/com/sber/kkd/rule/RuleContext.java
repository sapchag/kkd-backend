package com.sber.kkd.rule;

import com.sber.kkd.domain.Goal;
import com.sber.kkd.domain.KeyResult;
import com.sber.kkd.domain.Unit;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Снимок данных, над которым выполняются правила ККД.
 * Правила отделены от слоя API и доступа к данным: движок получает
 * снимок и возвращает список найденных проблем.
 *
 * @param units      все подразделения
 * @param goals      все цели
 * @param keyResults все ключевые результаты
 * @param runId      идентификатор прогона, для которого считается проверка
 */
public record RuleContext(
        List<Unit> units,
        List<Goal> goals,
        List<KeyResult> keyResults,
        Long runId
) {

    public List<Goal> activeGoals() {
        return goals.stream().filter(Goal::isActive).toList();
    }

    public List<KeyResult> krsFor(Long goalId) {
        return keyResults.stream()
                .filter(kr -> goalId.equals(kr.getGoalId()))
                .toList();
    }

    public Map<Long, List<KeyResult>> krsGroupedByGoal() {
        return keyResults.stream().collect(Collectors.groupingBy(KeyResult::getGoalId));
    }
}