package ru.hotel.hotel.repository;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import ru.hotel.hotel.domain.RoomReservation;

public interface ReservationRepository extends JpaRepository<RoomReservation, UUID> {
    @Query(
            """
            select count(s) > 0 from RoomReservation s
            where s.room.id = :roomId and s.active = true and s.startDate < :end and s.endDate > :start
            """)
    boolean overlaps(Long roomId, LocalDate start, LocalDate end);

    boolean existsByRoomId(Long roomId);

}
