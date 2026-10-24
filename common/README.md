# Common

Java-библиотека без собственного процесса и БД. Общие DTO `RoomView`, `ReservationCommand`, `ErrorDto`, `PageDto`; проверка диапазона дат; Resource Server JWT-политика; servlet security и корреляция. Бизнес-сервисы импортируют конфигурацию явно. Gateway использует только контракты и JwtSupport, сохраняя реактивный стек.

Сборка из корня репозитория: `./mvnw -pl common -am verify` (Windows: `.\mvnw.cmd -pl common -am verify`).
