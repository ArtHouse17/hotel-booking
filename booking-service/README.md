# Booking Service

Порт 8082. Пользователи и бронирования в H2. UserService отвечает за регистрацию/вход и ADMIN CRUD; BookingTransactions — за локальные транзакции, BookingService — за подтверждение и компенсацию, HotelPort — за HTTP-интеграцию.

Маршруты `/api/auth`, `/api/users`, `/api/bookings`. JWT проверяется Spring Security; доступ ограничен через `@PreAuthorize`. Hotel вызывается через Eureka с тайм-аутами и тремя попытками. Незавершённое освобождение можно повторить DELETE-запросом.

Сборка из корня: `mvnw.cmd -pl booking-service -am verify`. Запуск: `java -jar booking-service/target/booking-service-1.0.0.jar`. Swagger: `/swagger-ui.html`. Примеры и контракт — в корневом README.
