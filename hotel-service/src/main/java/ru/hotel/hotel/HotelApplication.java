package ru.hotel.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import ru.hotel.common.security.JwtConfiguration;
import ru.hotel.common.web.ServletSecurity;

@SpringBootApplication(scanBasePackages = {"ru.hotel.hotel", "ru.hotel.common.web"})
@Import({JwtConfiguration.class, ServletSecurity.class})
public class HotelApplication {
    public static void main(String[] args) {
        SpringApplication.run(HotelApplication.class, args);
    }
}
