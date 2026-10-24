# Бронирование отелей

Студенческий REST API на Java 17, Spring Boot 3.5.7, Spring Cloud 2025.0.0 и Maven. Реализованы функции из задания: JWT, роли USER/ADMIN, CRUD, подбор номера, двухшаговое бронирование с компенсацией и защита от повторных запросов.

## Модули

| Модуль | Порт | Назначение |
|---|---|---|
| discovery-server | 8761 | Eureka: регистрация и обнаружение сервисов |
| api-gateway | 8080 | маршрутизация запросов, проверка JWT |
| hotel-service | 8081 | отели, номера, занятость, статистика |
| booking-service | 8082 | пользователи, JWT, бронирования |
| common | — | общие DTO, ошибки и настройки безопасности |

Клиент → Gateway → Booking/Hotel. Адреса сервисов Gateway получает из Eureka. Booking обращается к Hotel через Spring Cloud LoadBalancer. У Booking и Hotel отдельные in-memory H2; после остановки JVM данные теряются. Gateway и Eureka не хранят прикладные данные.

## Запуск

Нужен JDK 17+. Maven Wrapper находится в проекте; при первом запуске нужен интернет для загрузки Maven и зависимостей.

Windows PowerShell, из корня:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17' # свой путь к JDK
.\mvnw.cmd clean verify
.\scripts\start.ps1 -SkipBuild
```

После запуска подождать около 30–60 секунд. Остановка: `.\scripts\stop.ps1`. Логи — в `.runtime/`.

Linux/macOS: `bash mvnw clean verify`, затем в отдельных терминалах запустить:

```bash
java -jar discovery-server/target/discovery-server-1.0.0.jar
java -jar hotel-service/target/hotel-service-1.0.0.jar
java -jar booking-service/target/booking-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

- [Swagger через Gateway](http://localhost:8080/swagger-ui.html).
- [Eureka](http://localhost:8761).
- Swagger отдельно: [Hotel](http://localhost:8081/swagger-ui.html), [Booking](http://localhost:8082/swagger-ui.html).

## Как пользоваться

Предзаполнены `admin / Admin123!`, `alice / Alice123!`, `bob / Bob12345!`. Администратор имеет ADMIN, остальные — USER. Есть два отеля и шесть номеров; номер 4 в ремонте.

1. Выполнить `POST /api/auth/login` с `{"username":"alice","password":"Alice123!"}`.
2. Скопировать `token`, нажать **Authorize** в Swagger и вставить токен. JWT действует один час. Каждый бизнес-сервис самостоятельно проверяет его.
3. Создать бронь `POST /api/bookings`:

```json
{
  "requestId": "588a215b-6319-43b1-9096-151e041c11a9",
  "autoSelect": true,
  "hotelId": 1,
  "startDate": "2027-06-10",
  "endDate": "2027-06-13"
}
```

Используйте будущие даты и новый UUID для новой брони. Для ручного выбора: `autoSelect=false` и `roomId=1`. При автоподборе `roomId` игнорируется; без `hotelId` поиск идёт по всем отелям.

4. Посмотреть собственную историю `GET /api/bookings` или отменить бронь `DELETE /api/bookings/{id}`.

## Эндпойнты

Публичный адрес — `http://localhost:8080`. Все запросы, кроме регистрации и входа, требуют Bearer JWT.

| Метод | Путь | Доступ |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | публично |
| POST, GET | `/api/bookings` | USER/ADMIN, собственные брони |
| GET, DELETE | `/api/bookings/{id}` | владелец |
| GET | `/api/bookings/recommendations` | USER/ADMIN |
| GET, POST | `/api/users` | ADMIN |
| GET, PATCH, DELETE | `/api/users/{id}` | ADMIN |
| GET, POST | `/api/hotels` | чтение USER/ADMIN, изменения ADMIN |
| GET, PUT, DELETE | `/api/hotels/{id}` | чтение USER/ADMIN, изменения ADMIN |
| GET, POST | `/api/rooms` | чтение USER/ADMIN, изменения ADMIN |
| GET, PUT, DELETE | `/api/rooms/{id}` | чтение USER/ADMIN, изменения ADMIN |
| GET | `/api/rooms/recommend` | USER/ADMIN |
| GET | `/api/rooms/statistics` | ADMIN |

Для свободных номеров и рекомендаций нужны `startDate`, `endDate`, необязателен `hotelId`. Статистика принимает необязательный `hotelId`. История, отели и пользователи поддерживают `page`, `size` (1–100). Отели также принимают `query` для поиска по имени/адресу.

Тела: отель — `{"name":"Отель","address":"Адрес"}`, номер — `{"hotelId":1,"number":"105","available":true}`, обновление номера — `{"number":"105","available":false}`, пользователь — `{"username":"student","password":"Password123!","role":"USER"}`. PATCH пользователя принимает любое из этих полей. Регистрация всегда создаёт USER.

Есть алиасы из задания: `POST /booking`, `GET /bookings`, `GET/DELETE /booking/{id}`, `POST /user/register`, `POST /user/auth`, `GET/POST /user`, `GET/PATCH/DELETE /user/{id}`.

## Как работает бронирование

1. Booking сохраняет PENDING локальной транзакцией.
2. Вызывает Hotel `POST /api/rooms/{id}/confirm-availability`. Hotel блокирует запись номера и проверяет пересечение дат. Интервал `[startDate, endDate)`, день выезда свободен для следующего заезда.
3. Успех → CONFIRMED. Сбой → CANCELLED и компенсирующий `POST /api/rooms/{id}/release`.

Внутренние confirm/release требуют SERVICE JWT, скрыты через Gateway. Их тело: `bookingId`, `startDate`, `endDate`. Глобальной транзакции нет. HTTP-вызовы происходят вне локальных транзакций БД.

Автоподбор сортирует свободные номера по `timesBooked`, затем по `id`; при конкурентном 409 пробует следующий. `available` означает эксплуатационную доступность, занятость учитывается отдельно по датам. При release счётчик уменьшается ровно один раз. Освобождённая запись сохраняется, поэтому запоздавшее подтверждение не восстановит отменённую бронь.

`requestId` обеспечивает идемпотентность: повтор не создаёт новую бронь, изменение параметров с тем же ключом даёт 409. Блокировки БД предотвращают пересекающиеся параллельные брони. Тайм-аут соединения 1 с, чтения 2 с; максимум три попытки для транспортных ошибок и 5xx, интервалы 100/200 мс. Ключевые действия логируются с `bookingId`, запросы — с `X-Request-Id`.

Коды: 201 — новая бронь, 200 — повтор/успешная отмена, 202 — ещё PENDING или освобождение пока не подтверждено, 400 — неверные данные, 401/403 — доступ, 404 — ресурс не найден, 409 — конфликт, 503 — Hotel недоступен. Ошибка саги возвращает отменённую бронь с `failureReason`; обычные ошибки — единый ErrorDto. Если release не удалось выполнить, повторный DELETE повторяет освобождение. Фоновых обработчиков нет.

Таблицы Booking: `users`, `bookings`. Hotel: `hotels`, `rooms`, `room_reservations` (локальная проекция дат бронирования). Основные поля соответствуют заданию; добавлены requestId, версия записи и флаг незавершённой компенсации. Удаление данных с историей даёт 409.

## Проверка и сдача

`mvnw.cmd clean verify` / `bash mvnw clean verify` запускает JUnit, MockMvc и WebTestClient. Тестируются JWT/роли, CRUD, даты, успешная бронь, сбой и тайм-аут Hotel, компенсация, повтор запроса и параллельные брони. Тесты используют отдельные H2 и не требуют запущенной Eureka.

Настройки находятся в `application.yml` каждого приложения. Учебный JWT-секрет одинаков во всех API-сервисах; его можно изменить переменной `JWT_SECRET`. `ADMIN_PASSWORD` меняет начальный пароль администратора, `app.seed=false` отключает демонстрационные данные.

Для сдачи загрузите проект в свой GitHub-репозиторий и отправьте ссылку через форму курса.
