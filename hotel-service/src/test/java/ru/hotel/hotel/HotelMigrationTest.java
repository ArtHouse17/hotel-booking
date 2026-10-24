package ru.hotel.hotel;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class HotelMigrationTest {
    @Test
    void migrationsCreateDemoInventoryAndDoNotRepeatInserts() {
        var source =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:hotel-migrations;DB_CLOSE_DELAY=-1", "sa", "");
        var flyway = Flyway.configure().dataSource(source).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);
        var jdbc = new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM hotels", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isEqualTo(6);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM rooms WHERE available = FALSE",
                                Integer.class))
                .isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isEqualTo(6);
        jdbc.update("INSERT INTO rooms (hotel_id, number) VALUES (1, '105')");
        assertThat(jdbc.queryForObject("SELECT id FROM rooms WHERE number='105'", Long.class))
                .isGreaterThan(6);
    }
}
