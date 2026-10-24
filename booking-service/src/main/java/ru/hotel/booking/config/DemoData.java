package ru.hotel.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.hotel.booking.domain.*;
import ru.hotel.booking.domain.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
public class DemoData implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String adminPassword;

    public DemoData(
            UserRepository users,
            PasswordEncoder passwords,
            @Value("${ADMIN_PASSWORD:Admin123!}") String adminPassword) {
        this.users = users;
        this.passwords = passwords;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.count() != 0) return;
        users.save(new User("admin", passwords.encode(adminPassword), Role.ADMIN));
        users.save(new User("alice", passwords.encode("Alice123!"), Role.USER));
        users.save(new User("bob", passwords.encode("Bob12345!"), Role.USER));
    }
}
