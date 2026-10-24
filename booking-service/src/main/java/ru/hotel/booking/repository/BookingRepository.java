package ru.hotel.booking.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import ru.hotel.booking.domain.*;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByUserIdAndRequestId(Long userId, UUID requestId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> lockById(UUID id);

}
