package ru.hotel.hotel.domain.repository;

import java.time.LocalDate;
import java.util.*;
import ru.hotel.hotel.domain.RoomReservation;

public interface ReservationRepository {
    Optional<RoomReservation> findById(UUID bookingId);
    boolean overlaps(Long roomId, LocalDate start, LocalDate end);
    boolean existsByRoomId(Long roomId);
    RoomReservation save(RoomReservation reservation);
}
