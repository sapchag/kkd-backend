package com.sber.kkd.rule;

/**
 * Проблема, найденная правилом ККД для одного прогона.
 * Абстракция, не зависящая от слоя данных; сохраняется в БД отдельно.
 *
 * @param ruleCode      код правила, например GOAL_NO_OWNER
 * @param objectRef     ссылка на объект-источник проблемы, например "goal:12" или "kr:7"
 * @param unitId        подразделение, к которому относится объект
 * @param severity      уровень важности: critical / major / minor
 * @param actualValue   фактическое значение (для отображения)
 * @param expectedValue ожидаемое значение
 * @param recommendation рекомендация по исправлению
 */
public record RuleIssue(
        String ruleCode,
        String objectRef,
        Long unitId,
        String severity,
        String actualValue,
        String expectedValue,
        String recommendation
) {
}