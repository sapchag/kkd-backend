# kkd-backend

Сервис модуля ККД (контроль качества данных) по целям сотрудников. Считает метрики качества данных по целям и ключевым результатам, хранит результаты прогонов в PostgreSQL и отдаёт их через REST API для страницы ККД.

Решение написано **с ИИ-ассистентом** (MultiTool). Спецификация, по которой шла работа: `specs/requirements.md` и `specs/design.md`.

## Стек

- Java 17+, Spring Boot 3, Spring Data JPA
- PostgreSQL 15
- Liquibase (миграции с откатом)
- Maven

## Быстрый старт

Всё поднимается одной командой:

```bash
docker compose up --build
```

После запуска:

- Swagger/документации нет, но эндпоинты доступны сразу
- Сервис: http://localhost:8080
- `/api/runs` — список прогонов
- `/api/kkd/summary?run_id=2&unit_id=1` — сводка по подразделению
- `/api/kkd/issues?run_id=2` — список проблем

Остановить: `docker compose down`. Удалить данные БД полностью: `docker compose down -v`.

## Запуск локально (без Docker)

Требуется PostgreSQL 15+ с базой `kkd`, пользователем `kkd`/паролем `kkd` (или задать через переменные окружения, см. `.env.example`).

```bash
# JDK 21 + Maven
mvn spring-boot:run
```

## Тесты

```bash
mvn test
```

Покрыты три сценария правил на граничных данных (`RulesTest`) и три сценария сервиса (`KkdServiceTest`): расчёт метрики с учётом дочерних подразделений, расчёт дельты, первый прогон (previous/delta = null), неуспешный прогон (409).

## Проверка наката → отката → повторного наката миграций

Каждый currentChangeSet в `src/main/resources/db/changelog/` снабжён блоком `rollback`. Проверка на чистой БД:

```bash
# 1. Накат
liquibase --url=jdbc:postgresql://localhost:5432/kkd --username=kkd --password=kkd \
  --changelog-file=src/main/resources/db/changelog/db.changelog-master.yaml update

# 2. Откат последнего changeSet
liquibase --url=jdbc:postgresql://localhost:5432/kkd --username=kkd --password=kkd \
  --changelog-file=src/main/resources/db/changelog/db.changelog-master.yaml rollbackCount 1

# 3. Повторный накат
liquibase --url=jdbc:postgresql://localhost:5432/kkd --username=kkd --password=kkd \
  --changelog-file=src/main/resources/db/changelog/db.changelog-master.yaml update
```

Все три шага выполняются без ошибок.

## Архитектура

Логика разделена на три слоя:

- **Правила ККД** — пакет `com.sber.kkd.rule`. Интерфейс `KkdRule` + компонент `RuleEngine`. Правила принимают чистое представление данных (`RuleContext`) и возвращают список проблем (`RuleIssue`). Не зависят от Spring Data и API.
- **Сервисный слой** — `com.sber.kkd.service`: `KkdService` (метрики и дельта), `IssueService` (список проблем, фильтры, пагинация), `RunService` (прогоны).
- **API слой** — `com.sber.kkd.api`: контроллер `KkdController` и обработчик ошибок `GlobalExceptionHandler`. Контроллер отделён от сервисов и доступа к данным.

### Метрики и каскадирование

`KkdService.summary` строит множество подразделений = выбранный узел + все потомки (каскадирование), берёт цели этого дерева и проблемы текущего и прошлого прогонов, и считает:

- `clean_goals_share` — доля корректных активных целей, % (лучше — выше)
- `issues_total` — всего найденных проблем (лучше — ниже)
- `critical_issues` — критических проблем (лучше — ниже)

Дельта «та же самая проблема» определяется как совпадение пары **правило + объект** (`ruleCode + objectRef`). У первого прогона сравнения нет — `compared_to`, `previous` и `delta` равны `null`, а не `0`.

### Модель данных

См. `specs/design.md`. Основные сущности: подразделения (`kkd_unit`, иерархия через `parent_id`), цели (`kkd_goal`), ключевые результаты (`kkd_kr`), справочник правил (`kkd_rule`), прогоны (`kkd_run`) и найденные проблемы (`kkd_issue`).

## Правила ККД (реализованы 4)

| Код | Важность | Условие нарушение |
|---|---|---|
| `GOAL_NO_OWNER` | critical | активная цель без ответственного |
| `GOAL_NO_KR` | critical | у активной цели нет ни одного КР |
| `KR_WEIGHTS_SUM` | major | сумма весов КР цели ≠ 100% |
| `CASCADE_NO_PARENT` | major | активная цель в подразделении-потомке не связана с целью вышестоящего уровня |