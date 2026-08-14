# task-tracker-service

Сервис управления задачами: REST API + PostgreSQL + Kafka. Java 21, Spring Boot 3.5, Spring Data JPA, Spring Kafka, Spring Security.

## Модель данных

- **Task**: id, title (наименование), description, status (`NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED`), assignee (User), version (optimistic locking)
- **User**: id, name, email

Онбординг пользователей вне scope задания — при первом запуске Flyway засеивает 5 тестовых пользователей (см. `GET /api/users`), их можно назначать исполнителями.

## Запуск

```bash
docker-compose up -d --build
```

Поднимает PostgreSQL, Kafka (KRaft, без ZooKeeper) и сам сервис. API доступен на `http://localhost:8080`, Swagger UI — `http://localhost:8080/swagger-ui.html` (открыт без авторизации).

> Порты Postgres (`5433`) и Kafka (`9092`) на хосте забинжены только на `127.0.0.1` — это бэкенд-хранилища, наружу торчать не должны. Порт Postgres сдвинут с дефолтного `5432`, чтобы не конфликтовать с локально установленным PostgreSQL.

## Авторизация

Все `/api/**` эндпоинты закрыты HTTP Basic Auth. Полноценный домен пользователей/ролей не строится — заданием онбординг явно вынесен за рамки, а модель `User(id, name, email)` не рассчитана на хранение credentials. Basic Auth — единственная сервисная учётка, чтобы API не было полностью анонимным.

Креды берутся из переменных окружения `API_USERNAME`/`API_PASSWORD`, дефолтов нет — без них приложение не запустится. В `docker-compose.yml` уже прописаны dev-значения (`apiuser` / `change-me-dev-password`) — **только для локального запуска**, для реального окружения их нужно заменить.

```bash
curl -u apiuser:change-me-dev-password localhost:8080/api/tasks
```

Swagger UI и `/actuator/health`, `/actuator/info` доступны без авторизации.

## REST API

| Метод | Путь                        | Описание                          |
|-------|-----------------------------|------------------------------------|
| GET   | `/api/tasks?page=&size=`    | Список задач с пагинацией (сортировка по id desc по умолчанию, максимум 100 записей за раз) |
| GET   | `/api/tasks/{id}`           | Задача по id                       |
| POST  | `/api/tasks`                | Создать задачу                     |
| PATCH | `/api/tasks/{id}/assignee`  | Назначить исполнителя              |
| PATCH | `/api/tasks/{id}/status`    | Сменить статус                     |
| GET   | `/api/users`                | Список пользователей (для выбора исполнителя) |

### Примеры

```bash
AUTH="-u apiuser:change-me-dev-password"

# Создать задачу
curl $AUTH -X POST localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Написать отчёт", "description": "Квартальный отчёт"}'

# Список задач
curl $AUTH "localhost:8080/api/tasks?page=0&size=10"

# Назначить исполнителя (id=1 — Alice Ivanova из сида)
curl $AUTH -X PATCH localhost:8080/api/tasks/1/assignee \
  -H "Content-Type: application/json" \
  -d '{"assigneeId": 1}'

# Сменить статус
curl $AUTH -X PATCH localhost:8080/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

Параллельное изменение одной и той же задачи (например, два одновременных `PATCH /status`) — второй запрос получает `409 Conflict` (оптимистическая блокировка через `@Version`), а не тихо теряет данные.

## Kafka-события

- `task.created` — публикуется после создания задачи
- `task.assignee-changed` — публикуется после назначения исполнителя

Ключ сообщения — `taskId`. Событие публикуется через `@TransactionalEventListener(phase = AFTER_COMMIT)` — только после успешного коммита в БД, поэтому невозможна ситуация «событие ушло, а задача не сохранилась». Обратная сторона этого выбора — доставка at-most-once: если процесс упадёт между коммитом и отправкой в Kafka, событие потеряется. Для тестового задания это осознанный компромисс; production-путь для at-least-once — Transactional Outbox.

`TaskAssigneeChangedEvent` содержит только `assigneeId` (без имени/email) — событие не должно тащить PII, которое может протухнуть или утечь в логи при сбое отправки.

```json
// task.created
{"taskId": 1, "title": "...", "description": "...", "occurredAt": "..."}
// task.assignee-changed
{"taskId": 1, "assigneeId": 1, "occurredAt": "..."}
```

## Тесты

```bash
mvn test
```

- `TaskServiceTest` — юнит-тесты сервисного слоя (Mockito)
- `TaskControllerIntegrationTest` — интеграционные тесты полного REST-флоу на Testcontainers (PostgreSQL + Kafka), включая проверку 401 без креденшлов

CI: `.github/workflows/ci.yml` гоняет `mvn verify` на JDK 21 при каждом push/PR.

## NFT и устойчивость к нагрузке

- До 100 000 задач: пагинация с явной сортировкой по `id` (без неё Postgres не гарантирует порядок строк между запросами — на активно изменяемой таблице это давало дубли/пропуски страниц), максимум 100 записей за один ответ (`spring.data.web.pageable.max-page-size`).
- До 10 000 пользователей: `GET /api/users` пока не пагинирован — при таком объёме отдаёт весь список одним ответом. Известное ограничение, не исправлено в этой итерации.
- `idx_tasks_assignee_id`/`idx_tasks_status` заложены под будущую фильтрацию (`GET /api/tasks?status=&assigneeId=` сейчас не реализован) — сами по себе текущие запросы (`findAll`, `findById`) их не используют.

## Безопасность: что сделано и что осталось

**Сделано:**
- HTTP Basic Auth на всех `/api/**`
- Отдельная непривилегированная Postgres-роль (`app_runtime`, только DML) для рантайма приложения — Flyway для DDL-миграций подключается отдельной учёткой-владельцем схемы (`spring.flyway.user/password`)
- Никаких паролей с дефолтами в коде — `DB_PASSWORD`, `API_PASSWORD` обязательны, без них приложение не стартует
- Порты Postgres/Kafka на хосте только на `127.0.0.1`
- Контейнер приложения — non-root пользователь, `.dockerignore`
- Лимиты на размер тела запроса и длину `description`, кап на размер страницы пагинации
- PII не публикуется в Kafka-событиях
- Актуальные версии зависимостей (Spring Boot 3.5.16 — закрывает известные CVE в Tomcat/Logback/pgjdbc/Spring Framework, актуальные на момент бампа)

**Осознанно не сделано в этой итерации (со сроками задания это не совместить без риска регрессий):**
- **Миграция на Spring Boot 4.x** — вышел в конце 2025, актуальная линия на сегодня, но требует проверки совместимости всего стека (Jackson 3, JUnit 6, новые дефолты Spring Security 7, springdoc). Делать это без выделенного времени на регрессионное тестирование — больший риск, чем оставаться на пропатченной 3.5.16 (финальный релиз линии, EOL 2026-06-30, но без открытых CVE на момент бампа).
- **Полноценная auth-модель с ролями** — текущая Basic Auth защищает API от анонимного доступа, но не разграничивает права между разными вызывающими. Для реального прод-сценария — OAuth2/JWT с ролями, привязанными к домену пользователей (потребует расширения модели `User`, которая сейчас описана заданием как `id, name, email`).
- **TLS/SASL на Kafka** — PLAINTEXT-листенеры, приемлемо для локального docker-compose, не для прода.
- **Rate limiting** — нет ни на уровне приложения, ни перед ним; для реального прода — на API Gateway/Ingress, либо Bucket4j + Redis для распределённой установки.
- **Идемпотентность `POST /api/tasks`** — повторный запрос (например, ретрай клиента после таймаута) создаёт дубль задачи и второе Kafka-событие. `Idempotency-Key` не реализован.
- **Валидация переходов статуса** — `PATCH /status` принимает любой переход (`DONE → NEW` пройдёт). В задании правила переходов не описаны; если появятся — это `EnumSet` в `TaskStatus`, не более.
