package ru.hotel.hotel.domain.repository;

import java.time.LocalDate;
import java.util.*;
import ru.hotel.hotel.domain.Room;

public interface RoomRepository {
    Optional<Room> findById(Long id);
    Optional<Room> lockById(Long id);
    List<Room> findAll();
    List<Room> findAvailable(Long hotelId, LocalDate start, LocalDate end);
    boolean existsByHotelId(Long hotelId);
    Room save(Room room);
    void delete(Room room);
}
