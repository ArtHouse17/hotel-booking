# Hotel Service

Порт 8081. CRUD отелей/номеров, рекомендации и счётчики бронирований. H2 хранит hotels, rooms и room_reservations. Проверка пересечения дат и изменение слота выполняются в одной локальной транзакции с блокировкой номера.

Маршруты `/api/hotels`, `/api/rooms`. Изменения и статистика — ADMIN, просмотр — USER/ADMIN; confirm/release — только внутренний SERVICE. При повторе confirm/release слот и счётчик не меняются повторно.

Сборка из корня: `mvnw.cmd -pl hotel-service -am verify`. Запуск: `java -jar hotel-service/target/hotel-service-1.0.0.jar`. Swagger: `/swagger-ui.html`. Примеры — в корневом README.
