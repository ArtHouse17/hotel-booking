# API Gateway

Порт 8080. Spring Cloud Gateway маршрутизирует бронирования, пользователей и авторизацию в `lb://booking-service`, отели/номера — в `lb://hotel-service`. Адреса получает из Eureka. Проверяет JWT и передаёт его в сервисы. Внутренние confirm/release скрыты.

Swagger `/swagger-ui.html` показывает оба API. Заголовок `X-Request-Id` передаётся по цепочке запросов.

Сборка из корня: `mvnw.cmd -pl api-gateway -am verify`. Запуск: `java -jar api-gateway/target/api-gateway-1.0.0.jar`.

E2E: при запущенном Docker Compose выполните из корня `mvnw.cmd -Pe2e verify`. Набор `HotelBookingE2EIT` проверяет регистрацию и роли, CRUD, ручное бронирование и автоподбор, компенсацию отказа Hotel, приватность, повторную доставку и параллельные бронирования. Он использует только публичный HTTP API и ждёт готовности сервисов до 90 секунд. Другой адрес Gateway задаётся через `-De2e.base-url=http://localhost:8080`. Сетевые ошибки и тайм-ауты отдельно проверяются в тестах Booking Service и HTTP-клиента.
