package ru.hotel.booking.domain.repository;

import java.util.*;
import ru.hotel.booking.domain.Booking;
import ru.hotel.common.PageDto;

public interface BookingRepository {
    Optional<Booking> findById(UUID id);
    Optional<Booking> lockById(UUID id);
    Optional<Booking> findByUserIdAndRequestId(Long userId, UUID requestId);
    PageDto<Booking> findByUserId(Long userId, int page, int size);
    boolean existsByUserId(Long userId);
    Booking save(Booking booking);
}
