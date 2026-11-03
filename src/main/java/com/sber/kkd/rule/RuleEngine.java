package com.sber.kkd.rule;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Движок правил ККД: применяет все зарегистрированные правила к снимку данных
 * и собирает единый список найденных проблем. Отделён от слоя API и доступа к данным.
 */
@Component
public class RuleEngine {

    private final List<KkdRule> rules;

    public RuleEngine(List<KkdRule> rules) {
        this.rules = rules;
    }

    public List<RuleIssue> evaluate(RuleContext context) {
        List<RuleIssue> result = new ArrayList<>();
        for (KkdRule rule : rules) {
            result.addAll(rule.evaluate(context));
        }
        return result;
    }
}