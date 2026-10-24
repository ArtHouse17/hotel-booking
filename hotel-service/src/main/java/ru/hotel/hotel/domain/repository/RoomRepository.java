package ru.hotel.hotel.domain.repository;

import ru.hotel.hotel.domain.Room;

import java.time.LocalDate;
import java.util.*;

public interface RoomRepository {
    Optional<Room> findById(Long id);

    Optional<Room> lockById(Long id);

    List<Room> findAll();

    List<Room> findAvailable(Long hotelId, LocalDate start, LocalDate end);

    boolean existsByHotelId(Long hotelId);

    Room save(Room room);

    void delete(Room room);
}
