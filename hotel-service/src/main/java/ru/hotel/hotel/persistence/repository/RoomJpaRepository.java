package ru.hotel.hotel.persistence.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import ru.hotel.hotel.persistence.entity.RoomEntity;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoomEntity r where r.id = :id")
    Optional<RoomEntity> lockById(Long id);

    boolean existsByHotelId(Long hotelId);

    @Query(
            """
            select r from RoomEntity r join fetch r.hotel
            where r.available = true and (:hotelId is null or r.hotel.id = :hotelId)
            and not exists (select s.bookingId from RoomReservationEntity s
                where s.room = r and s.active = true and s.startDate < :end and s.endDate > :start)
            """)
    List<RoomEntity> findAvailable(Long hotelId, LocalDate start, LocalDate end);
}
