package ru.hotel.hotel.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.hotel.domain.*;
import ru.hotel.hotel.domain.repository.*;

@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
public class DemoData implements CommandLineRunner {
    private final HotelRepository hotels;
    private final RoomRepository rooms;

    public DemoData(HotelRepository hotels, RoomRepository rooms) {
        this.hotels = hotels;
        this.rooms = rooms;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (hotels.count() != 0) return;
        var first = hotels.save(new Hotel("Aurora", "Moscow, Tverskaya 10"));
        var second = hotels.save(new Hotel("Neva", "Saint Petersburg, Nevsky 20"));
        rooms.save(new Room(first, "101", true));
        rooms.save(new Room(first, "102", true));
        rooms.save(new Room(first, "103", true));
        rooms.save(new Room(first, "104-repair", false));
        rooms.save(new Room(second, "201", true));
        rooms.save(new Room(second, "202", true));
    }
}
