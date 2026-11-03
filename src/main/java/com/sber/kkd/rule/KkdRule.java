package com.sber.kkd.rule;

import java.util.List;

/**
 * Интерфейс правила проверки качества данных.
 * Правило получает весь снимок данных и возвращает список найденных проблем.
 */
public interface KkdRule {

    String code();

    List<RuleIssue> evaluate(RuleContext context);
}