# task-tracker-service

Сервис управления задачами: REST API + PostgreSQL + Kafka. Java 21, Spring Boot 3.3, Spring Data JPA, Spring Kafka.

## Модель данных

- **Task**: id, title (наименование), description, status (`NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED`), assignee (User)
- **User**: id, name, email

Онбординг/авторизация пользователей вне scope — при первом запуске Flyway засеивает 5 тестовых пользователей (см. `GET /api/users`), их можно назначать исполнителями.

## Запуск

```bash
docker-compose up -d --build
```

Поднимает PostgreSQL, Kafka (KRaft, без ZooKeeper) и сам сервис. API доступен на `http://localhost:8080`, Swagger UI — `http://localhost:8080/swagger-ui.html`.

> Порт Postgres на хосте — `5433` (внутри docker-сети — обычный `5432`), чтобы не конфликтовать с локально установленным PostgreSQL.

## REST API

| Метод | Путь                        | Описание                          |
|-------|-----------------------------|------------------------------------|
| GET   | `/api/tasks?page=&size=`    | Список задач с пагинацией          |
| GET   | `/api/tasks/{id}`           | Задача по id                       |
| POST  | `/api/tasks`                | Создать задачу                     |
| PATCH | `/api/tasks/{id}/assignee`  | Назначить исполнителя              |
| PATCH | `/api/tasks/{id}/status`    | Сменить статус                     |
| GET   | `/api/users`                | Список пользователей (для выбора исполнителя) |

### Примеры

```bash
# Создать задачу
curl -X POST localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Написать отчёт", "description": "Квартальный отчёт"}'

# Список задач
curl "localhost:8080/api/tasks?page=0&size=10"

# Назначить исполнителя (id=1 — Alice Ivanova из сида)
curl -X PATCH localhost:8080/api/tasks/1/assignee \
  -H "Content-Type: application/json" \
  -d '{"assigneeId": 1}'

# Сменить статус
curl -X PATCH localhost:8080/api/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

## Kafka-события

- `task.created` — публикуется при создании задачи
- `task.assignee-changed` — публикуется при назначении исполнителя

Ключ сообщения — `taskId`, значение — JSON-payload события.

## Тесты

```bash
mvn test
```

- `TaskServiceTest` — юнит-тесты сервисного слоя (Mockito)
- `TaskControllerIntegrationTest` — интеграционные тесты полного REST-флоу на Testcontainers (PostgreSQL + Kafka)

## NFT

Индексы на `tasks.assignee_id` и `tasks.status` рассчитаны на нагрузку до 10 000 пользователей / 100 000 задач без дополнительного шардирования.
