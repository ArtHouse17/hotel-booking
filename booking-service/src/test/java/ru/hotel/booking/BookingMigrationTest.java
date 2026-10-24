package ru.hotel.booking;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BookingMigrationTest {
    @Test
    void migrationsCreateDemoUsersWithWorkingPasswordsAndDoNotRepeatInserts() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:booking-migrations;DB_CLOSE_DELAY=-1", "sa", "");
        var flyway = Flyway.configure().dataSource(source).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        var jdbc = new JdbcTemplate(source);
        var passwords = new BCryptPasswordEncoder();
        assertThat(passwords.matches("Admin123!", jdbc.queryForObject("SELECT password FROM users WHERE username='admin'", String.class))).isTrue();
        assertThat(passwords.matches("Alice123!", jdbc.queryForObject("SELECT password FROM users WHERE username='alice'", String.class))).isTrue();
        assertThat(passwords.matches("Bob12345!", jdbc.queryForObject("SELECT password FROM users WHERE username='bob'", String.class))).isTrue();
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE username='admin'", String.class)).isEqualTo("ADMIN");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(3);
        jdbc.update("INSERT INTO users (username, password, role) VALUES ('newuser', 'hash', 'USER')");
        assertThat(jdbc.queryForObject("SELECT id FROM users WHERE username='newuser'", Long.class)).isGreaterThan(3);
    }
}
