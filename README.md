# Бронирование отелей

REST API на Spring Boot: пользователи, отели, номера и бронирования. JWT, роли USER/ADMIN, автоподбор номера и отмена брони при сбое подтверждения.

Стек: Java 17, Spring Boot 3.5, Spring Cloud, Spring Security, JPA, H2, Maven.

## Запуск

Нужен Docker с Docker Compose. Из корня проекта:

```bash
docker compose up --build
```

Остановка: `docker compose down`. Данные H2 сбрасываются после остановки сервисов.

После запуска подождите около 30–60 секунд. [Swagger](http://localhost:8080/swagger-ui.html) содержит все эндпойнты и схемы запросов. [Eureka](http://localhost:8761) показывает зарегистрированные сервисы.

## Использование

Тестовые аккаунты: `alice / Alice123!` (USER), `admin / Admin123!` (ADMIN).

Таблицы и начальные данные создаются Flyway-миграциями в `src/main/resources/db/migration` каждого бизнес-сервиса. Пароли пользователей хранятся как BCrypt-хеши.

В Swagger выполните `POST /api/auth/login`, скопируйте `token` и вставьте его в **Authorize**.

- `/api/auth/register`, `/api/auth/login` — регистрация и вход.
- `/api/bookings` — создание и просмотр своих бронирований; `GET/DELETE /api/bookings/{id}` — просмотр и отмена.
- `/api/hotels`, `/api/rooms` — просмотр; создание, изменение и удаление доступны ADMIN.
- `GET /api/rooms/recommend` — свободные номера по числу бронирований.
- `GET /api/rooms/statistics`, `/api/users` — статистика и управление пользователями для ADMIN.

При создании брони укажите будущие `startDate`, `endDate` и уникальный UUID в `requestId`. Для автоподбора — `autoSelect=true`, для ручного выбора — `autoSelect=false` и `roomId`.

## Тесты

Тесты запускаются при сборке Docker-образов. Отдельно с JDK 17+: `mvnw.cmd verify` (Windows) или `bash mvnw verify`.
