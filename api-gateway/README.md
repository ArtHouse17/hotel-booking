# API Gateway

Порт 8080. Spring Cloud Gateway маршрутизирует бронирования, пользователей и авторизацию в `lb://booking-service`, отели/номера — в `lb://hotel-service`. Адреса получает из Eureka. Проверяет JWT и передаёт его в сервисы. Внутренние confirm/release скрыты.

Swagger `/swagger-ui.html` показывает оба API. Заголовок `X-Request-Id` передаётся по цепочке запросов.

Сборка из корня: `mvnw.cmd -pl api-gateway -am verify`. Запуск: `java -jar api-gateway/target/api-gateway-1.0.0.jar`.
