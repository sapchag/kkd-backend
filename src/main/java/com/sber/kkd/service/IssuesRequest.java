package com.sber.kkd.service;

/**
 * Параметры запроса списка проблем (GET /api/kkd/issues).
 *
 * @param ruleCode  фильтр по коду правила (nullable)
 * @param severity  фильтр по важности: critical / major / minor (nullable)
 * @param unitCode  фильтр по коду подразделения (nullable)
 * @param deltaStatus фильтр по статусу дельты: new / persisting / resolved (nullable)
 */
public record IssuesRequest(
        String ruleCode,
        String severity,
        String unitCode,
        String deltaStatus
) {
}