# Booking Service

Порт 8082. Домен содержит модели и интерфейсы репозиториев; persistence — JPA-сущности, маппинг и адаптеры H2. Сервисы работают с доменом. Booking управляет переходами состояния, BookingService — подтверждением и компенсацией, HotelPort — HTTP-интеграцией.

Маршруты `/api/auth`, `/api/users`, `/api/bookings`. JWT проверяется Spring Security; доступ ограничен через `@PreAuthorize`. Hotel вызывается через Eureka с тайм-аутами и тремя попытками. Незавершённое освобождение можно повторить DELETE-запросом.

Сборка из корня: `mvnw.cmd -pl booking-service -am verify`. Запуск: `java -jar booking-service/target/booking-service-1.0.0.jar`. Swagger: `/swagger-ui.html`. Примеры и контракт — в корневом README.
