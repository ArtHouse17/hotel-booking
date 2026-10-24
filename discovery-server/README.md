# Discovery Server

Eureka на 8761, имя `discovery-server`. Не регистрирует себя. Клиенты обновляют реестр динамически; Gateway и Booking используют имена сервисов и Spring Cloud LoadBalancer. Прикладной БД нет. Eureka предназначена для внутренней доверенной сети, локально слушает 127.0.0.1.

Сборка из корня: `mvnw.cmd -pl discovery-server -am package`. Запуск: `java -jar discovery-server/target/discovery-server-1.0.0.jar`. Dashboard: `http://localhost:8761`, health: `/actuator/health`.
