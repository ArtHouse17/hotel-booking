package ru.hotel.hotel.persistence.repository;

import org.springframework.data.jpa.repository.*;

import ru.hotel.hotel.persistence.entity.RoomReservationEntity;

import java.time.LocalDate;
import java.util.*;

public interface ReservationJpaRepository extends JpaRepository<RoomReservationEntity, UUID> {
    @Query(
            """
select count(s) > 0 from RoomReservationEntity s
where s.room.id = :roomId and s.active = true and s.startDate < :end and s.endDate > :start
""")
    boolean overlaps(Long roomId, LocalDate start, LocalDate end);

    boolean existsByRoomId(Long roomId);
}
