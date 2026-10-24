# Hotel Service

Порт 8081. Домен содержит модели и интерфейсы репозиториев; persistence — JPA-сущности, маппинг и адаптеры H2. Сервисы работают с доменом, Room и RoomReservation управляют счётчиком и освобождением слота. Проверка дат и сохранение выполняются в локальной транзакции с блокировкой номера.

Маршруты `/api/hotels`, `/api/rooms`. Изменения и статистика — ADMIN, просмотр — USER/ADMIN; confirm/release — только внутренний SERVICE. При повторе confirm/release слот и счётчик не меняются повторно.

Сборка из корня: `mvnw.cmd -pl hotel-service -am verify`. Запуск: `java -jar hotel-service/target/hotel-service-1.0.0.jar`. Swagger: `/swagger-ui.html`. Примеры — в корневом README.
