package ru.hotel.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import ru.hotel.common.security.JwtConfiguration;
import ru.hotel.common.web.ServletSecurity;

@SpringBootApplication(scanBasePackages = {"ru.hotel.booking", "ru.hotel.common.web"})
@Import({JwtConfiguration.class, ServletSecurity.class})
public class BookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
