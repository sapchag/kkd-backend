package com.sber.kkd.api.dto;

import java.util.List;

/**
 * Ответ GET /api/kkd/issues — список проблем прогона с пагинацией и фильтрами.
 */
public record IssuesPageDto(
        List<IssueDto> items,
        long total,
        int limit,
        int offset
) {
}