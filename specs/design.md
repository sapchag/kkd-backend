# design.md

Модель данных, API и ключевые решения.

## Модель данных

### Подразделения (`kkd_unit`)

| Поле | Тип | Описание |
|---|---|---|
| `id` | bigint PK | идентификатор |
| `code` | varchar(32), unique | код, напр. `HQ-CIB` |
| `name` | varchar(255) | название |
| `parent_id` | bigint FK → kkd_unit | родительское подразделение (null — корневое) |

### Цели (`kkd_goal`)

| Поле | Тип | Описание |
|---|---|---|
| `id` | bigint PK | идентификатор |
| `code` | varchar(32), unique | код |
| `unit_id` | bigint FK | подразделение |
| `owner` | varchar(128), nullable | ответственный |
| `parent_goal_id` | bigint FK | родительская цель (каскадирование) |
| `status` | varchar(16) | `ACTIVE` / `CLOSED` / `DRAFT` |
| `due_date` | date, nullable | срок |

### Ключевые результаты (`kkd_kr`)

| Поле | Тип | Описание |
|---|---|---|
| `id` | bigint PK | идентификатор |
| `goal_id` | bigint FK | цель |
| `target_value` | numeric(18,2), nullable | целевое значение |
| `actual_value` | numeric(18,2), nullable | фактическое значение |
| `weight` | numeric(8,4) | вес, % (50 = 50%) |
| `fact_updated_at` | timestamp, nullable | дата обновления факта |

### Справочник правил (`kkd_rule`)

| Поле | Тип | Описание |
|---|---|---|
| `code` | varchar(32) PK | код правила |
| `name` | varchar(128) | имя |
| `severity` | varchar(16) | critical / major / minor |

### Прогоны (`kkd_run`)

| Поле | Тип | Описание |
|---|---|---|
| `id` | bigint PK | идентификатор |
| `code` | varchar(16), unique | код, напр. `RUN-002` |
| `snapshot_date` | date | дата снимка данных |
| `status` | varchar(16) | SUCCESS / FAILED |
| `compared_to_run_id` | bigint FK | прогон-предшественник (для сравнения) |

### Найденные проблемы (`kkd_issue`)

| Поле | Тип | Описание |
|---|---|---|
| `id` | bigint PK | идентификатор |
| `run_id` | bigint FK | прогон |
| `rule_code` | varchar(32) FK | правило |
| `unit_id` | bigint FK, nullable | подразделение |
| `object_ref` | varchar(64) | ссылка на объект, напр. `goal:12`, `kr:7` |
| `severity` | varchar(16) | важность |
| `actual_value` | varchar(255), nullable | фактическое значение |
| `expected_value` | varchar(255), nullable | ожидаемое |
| `recommendation` | varchar(512), nullable | рекомендация |

### Правила формализации (как активная цель)

Активная цель = `kkd_goal.status = 'ACTIVE'`. Правила применяются только к активным целям и их КР.

Формализованные правила (SQL-условие нарушения):

- **GOAL_NO_OWNER**: `status='ACTIVE' AND (owner IS NULL OR owner = '')`
- **GOAL_NO_KR**: `status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM kkd_kr WHERE goal_id = g.id)`
- **KR_WEIGHTS_SUM**: `status='ACTIVE' AND COUNT(kr) > 0 AND SUM(kr.weight) <> 100`
- **CASCADE_NO_PARENT**: `status='ACTIVE' AND parent_goal_id IS NULL AND unit.parent_id IS NOT NULL`
  (цель в подразделении-потомке без связи с целью вышестоящего уровня)

## Метрики

Для выбранного подразделения строится множество = сам узел + все потомки (каскадирование). Метрики считаются по целям и проблемам этого дерева.

### 1. `clean_goals_share` — доля корректных целей, %

```
value = clean_active_goals / active_goals * 100
```

- активная цель «корректная», если ни одна найденная проблема не указывает на неё (`object_ref = 'goal:<id>'`).
- Лучше — **выше**.
- `unit = "%"`, `higher_is_better = true`.

### 2. `issues_total` — всего найденных проблем

```
value = count(problem for subtree)
```

- Лучше — **ниже**.

### 3. `critical_issues` — критических проблем

```
value = count(problem with severity='critical' for subtree)
```

- Лучше — **ниже**.

Для каждой метрики с прошлым прогоном: `previous`, `delta = value − previous`, `trend ∈ {better, worse, same}`. Направление дельты и «лучше/хуже» — разные вещи: `trend` вычисляется через `higher_is_better`.

## Дельта

- Проблема «та же самая» идентифицируется парой `rule_code + object_ref`.
- При сравнении прогонов:
  - **new** — ключ есть в текущем, нет в прошлом;
  - **persisting** — ключ есть в обоих;
  - **resolved** — ключ есть в прошлом, нет в текущем.
- У первого прогона `compared_to = null`, `previous = null`, `delta = null` (не `0`).
- Если прошлый прогон `FAILED` — он игнорируется как недостоверный, сравнения нет.

## API

### GET `/api/runs`

```json
[
  {
    "id": "RUN-002",
    "snapshotDate": "2026-10-26",
    "status": "SUCCESS",
    "comparedTo": "RUN-001",
    "issueCount": 8
  }
]
```

### GET `/api/kkd/summary?run_id=&unit_id=`

Параметры: `run_id` (id прогона), `unit_id` (id подразделения). Ответ — см. полный пример ниже.

### GET `/api/kkd/issues?run_id=&rule=&severity=&unit=&delta=&limit=&offset=`

Параметры:

- `run_id` — обязательный (id прогона);
- `rule` — фильтр по коду правила (опционально);
- `severity` — important: critical / major / minor;
- `unit` — фильтр по коду подразделения;
- `delta` — фильтр по статусу дельты: new / persisting / resolved;
- `limit` — размер страницы (1..100, по умолчанию 20);
- `offset` — смещение (>=0, по умолчанию 0).

Ответ:

```json
{
  "items": [
    {
      "id": 13,
      "rule": "GOAL_NO_KR",
      "severity": "critical",
      "objectRef": "goal:3",
      "unitId": "HQ-CIB",
      "actualValue": "0",
      "expectedValue": ">=1",
      "recommendation": "Добавить хотя бы один ключевой результат",
      "deltaStatus": "persisting"
    }
  ],
  "total": 8,
  "limit": 20,
  "offset": 0
}
```

### Коды ошибок

Формат при любой ошибке: `{"error": {"code": "...", "message": "..."}}`.

| Ситуация | HTTP | code |
|---|---|---|
| неизвестный прогон или подразделение | 404 | `NOT_FOUND` |
| некорректный параметр (limit/offset/тип) | 422 | `INVALID_PARAM` |
| прогон завершился с ошибкой | 409 | `RUN_FAILED` |

### Пример ответа `GET /api/kkd/summary` целиком

`GET /api/kkd/summary?run_id=2&unit_id=1`

```json
{
  "run": { "id": "RUN-002", "snapshotDate": "2026-10-26" },
  "comparedTo": { "id": "RUN-001", "snapshotDate": "2026-10-19" },
  "unit": { "id": "HQ-CIB", "name": "Департамент корпоративного бизнеса" },
  "metrics": [
    {
      "key": "clean_goals_share",
      "title": "Доля корректных целей",
      "unit": "%",
      "value": 50.0,
      "previous": 42.9,
      "delta": 7.1,
      "trend": "better",
      "higherIsBetter": true
    },
    {
      "key": "issues_total",
      "title": "Найдено проблем",
      "unit": "шт.",
      "value": 8.0,
      "previous": 9.0,
      "delta": -1.0,
      "trend": "better",
      "higherIsBetter": false
    },
    {
      "key": "critical_issues",
      "title": "Критические проблемы",
      "unit": "шт.",
      "value": 5.0,
      "previous": 5.0,
      "delta": 0.0,
      "trend": "same",
      "higherIsBetter": false
    }
  ],
  "issuesBySeverity": [
    { "code": "critical", "current": 5, "previous": 5, "delta": 0 },
    { "code": "major", "current": 3, "previous": 4, "delta": -1 }
  ],
  "delta": { "newIssues": 3, "persisting": 5, "resolved": 4 }
}
```

### Граничные случаи

| Случай | Поведение |
|---|---|
| первый прогон, сравнивать не с чем | `compared_to`, `previous`, `delta`, `trend` = `null` |
| прошлый прогон завершился с ошибкой | сравнение пропускается, дельта отсутствует |
| подразделение без целей | цель-метрика `clean_goals_share` = `null` (знаменатель 0) |
| неизвестные id | 404 `NOT_FOUND` |
| неверный параметр | 422 `INVALID_PARAM` |
| прогон ещё выполняется / FAILED | 409 `RUN_FAILED` |

## Ключевые решения

- **Правила отделены от данных и API**: компонент `RuleEngine` получает чистый снимок (`RuleContext`) и возвращает `RuleIssue`. Без Spring Data в правилах — их легко покрыть unit-тестами и переиспользовать.
- **Сущность проблемы в БД** хранит фактическое/ожидаемое значение и рекомендацию — чтобы не пересчитывать на фронтенде.
- **Каскадирование** реализовано обходом дерева `kkd_unit` в памяти, а не рекурсивным SQL, чтобы схема оставалась простой.
- **Метрики не пересчитываются на фронтенде**: все числа отдаются из API как есть.
- **Идентичность проблемы** = `rule_code + object_ref`, стабильная между прогонами.
- **Липкие данные**: прогоны с проблемами зафиксированы в миграции, чтобы демо-данные не менялись при перезапуске.