package ru.hotel.booking.persistence.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import ru.hotel.booking.persistence.entity.*;

import java.util.*;

public interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {
    Optional<BookingEntity> findByUserIdAndRequestId(Long userId, UUID requestId);

    Page<BookingEntity> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingEntity b where b.id = :id")
    Optional<BookingEntity> lockById(UUID id);
}
