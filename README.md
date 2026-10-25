# Reservation Service

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6db33f)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791)
![Maven](https://img.shields.io/badge/Maven-3-c71a36)
![Docker](https://img.shields.io/badge/Docker-PostgreSQL-2496ED)

REST API для бронирования комнат. Пользователь создаёт заявку на бронь, менеджер подтверждает или отклоняет её. Сервис не даёт подтвердить бронь, если комната на эти даты уже занята.

## Стек

- Java 21
- Spring Boot 3 (Web, Data JPA, Validation)
- Hibernate
- PostgreSQL (запускается в Docker)
- Maven

## Как работает бронь

У брони три статуса: `PENDING`, `APPROVED`, `CANCELLED`.

1. Новая бронь всегда создаётся в статусе `PENDING`. Передавать `id` и `status` при создании нельзя.
2. Даты брони не могут быть в прошлом, дата окончания должна быть позже даты начала.
3. Изменить можно только бронь в статусе `PENDING`.
4. При подтверждении сервис ищет пересечения с уже подтверждёнными бронями этой комнаты. Если даты пересекаются, подтверждение отклоняется.
5. Отменить можно только неподтверждённую бронь. Подтверждённую отменяет менеджер.

Пересечение дат проверяется одним JPQL-запросом: две брони пересекаются, если `start1 < end2` и `start2 < end1`.

## API

| Метод | URL | Описание |
|---|---|---|
| GET | `/reservation/{id}` | Получить бронь по id |
| GET | `/reservation?roomId=&userId=&pageSize=&pageNumber=` | Список броней с фильтрами и пагинацией (по умолчанию 10 записей, страница 0) |
| POST | `/reservation` | Создать бронь |
| PUT | `/reservation/{id}` | Изменить бронь |
| DELETE | `/reservation/{id}/cancel` | Отменить бронь |
| POST | `/reservation/{id}/approve` | Подтвердить бронь |
| POST | `/reservation/availability/check?roomId=&startDate=&endDate=` | Проверить, свободна ли комната на даты |

Пример тела запроса на создание:

```json
{
  "userId": 1,
  "roomId": 3,
  "startDate": "2026-11-10",
  "endDate": "2026-11-12"
}
```

Ошибки возвращаются в одном формате через `@ControllerAdvice`:

```json
{
  "message": "Bad request",
  "detailedMessage": "Cannot approve reservation because of conflict",
  "errorTime": "2026-11-01T12:00:00"
}
```

- `404` — бронь не найдена
- `400` — ошибка валидации или недопустимый переход статуса
- `500` — остальные ошибки

## Запуск

1. Поднять PostgreSQL:

```bash
docker run --name reservation-db -e POSTGRES_PASSWORD=password -p 5434:5432 -d postgres:16
```

2. Запустить приложение:

```bash
cd reservation-system
./mvnw spring-boot:run
```

Сервис будет доступен на `http://localhost:8070`. Таблицы создаются автоматически (`ddl-auto=update`).

## Структура

```
reservations/               бронирования: контроллер, сервис, репозиторий, маппер
reservations/availabitily/  проверка доступности комнаты
web/                        обработка ошибок
```

## Что хочу доработать

- перейти с `ddl-auto` на миграции Liquibase
- добавить интеграционные тесты на Testcontainers
- описать API в OpenAPI/Swagger
