package ru.hotel.booking.service;

import java.util.*;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.booking.api.*;
import ru.hotel.booking.api.BookingDtos.*;
import ru.hotel.booking.domain.*;
import ru.hotel.booking.repository.*;
import ru.hotel.common.*;

/** Each public mutation is one short local transaction; no HTTP call holds a DB lock. */
@Service
@Transactional
public class BookingTransactions {
    private static final Logger log = LoggerFactory.getLogger(BookingTransactions.class);
    private final BookingRepository bookings;
    private final UserRepository users;
    private final BookingMapper mapper;

    public BookingTransactions(
            BookingRepository bookings, UserRepository users, BookingMapper mapper) {
        this.bookings = bookings;
        this.users = users;
        this.mapper = mapper;
    }

    public BookingOutcome begin(Long userId, CreateBooking input) {
        DateRange.validate(input.startDate(), input.endDate());
        if (!input.autoSelect() && input.roomId() == null)
            throw ApiException.badRequest("roomId is required when autoSelect=false");
        // Serializing requests per user makes create-or-replay atomic, also across service
        // instances.
        var user =
                users.lockById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        var existing = bookings.findByUserIdAndRequestId(userId, input.requestId());
        if (existing.isPresent()) {
            if (!existing.get().getFingerprint().equals(input.fingerprint()))
                throw ApiException.conflict("requestId was already used with different parameters");
            return new BookingOutcome(mapper.booking(existing.get()), false);
        }
        var booking = new Booking();
        booking.setUser(user);
        booking.setRequestId(input.requestId());
        booking.setFingerprint(input.fingerprint());
        booking.setRoomId(input.autoSelect() ? null : input.roomId());
        booking.setHotelId(input.hotelId());
        booking.setAutoSelect(input.autoSelect());
        booking.setStartDate(input.startDate());
        booking.setEndDate(input.endDate());
        bookings.saveAndFlush(booking);
        log.info(
                "event=booking_state bookingId={} requestId={} state=PENDING",
                booking.getId(),
                booking.getRequestId());
        return new BookingOutcome(mapper.booking(booking), true);
    }

    public BookingView selectRoom(UUID id, Long roomId) {
        var b = lock(id);
        if (b.getStatus() != BookingStatus.PENDING)
            throw ApiException.conflict("Booking is no longer pending");
        b.setRoomId(roomId);
        return mapper.booking(b);
    }

    public BookingView confirm(UUID id) {
        var b = lock(id);
        if (b.getStatus() != BookingStatus.PENDING)
            throw ApiException.conflict("Booking was cancelled while confirming");
        b.setStatus(BookingStatus.CONFIRMED);
        log.info("event=booking_state bookingId={} state=CONFIRMED", id);
        return mapper.booking(b);
    }

    public BookingView cancel(UUID id, String reason, int status) {
        var b = lock(id);
        if (b.getStatus() != BookingStatus.CANCELLED) {
            b.setStatus(BookingStatus.CANCELLED);
            b.setFailureReason(reason);
            b.setFailureStatus(status);
            b.setCompensationRequired(b.getRoomId() != null);
            log.info(
                    "event=booking_state bookingId={} state=CANCELLED compensationRequired={}",
                    id,
                    b.isCompensationRequired());
        }
        return mapper.booking(b);
    }

    public void compensated(UUID id) {
        var b = lock(id);
        if (b.getStatus() == BookingStatus.CANCELLED) b.setCompensationRequired(false);
        log.info("event=compensation_complete bookingId={}", id);
    }

    @Transactional(readOnly = true)
    public BookingView get(UUID id, Long userId) {
        var b = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        // Returning 404 also prevents enumeration of another user's bookings.
        if (!b.getUser().getId().equals(userId)) throw ApiException.notFound("Booking not found");
        return mapper.booking(b);
    }

    @Transactional(readOnly = true)
    public BookingView internal(UUID id) {
        return mapper.booking(
                bookings.findById(id)
                        .orElseThrow(() -> ApiException.notFound("Booking not found")));
    }

    @Transactional(readOnly = true)
    public PageDto<BookingView> history(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw ApiException.badRequest("page >= 0; size must be 1..100");
        var result =
                bookings.findByUserId(
                        userId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PageDto<>(
                result.map(mapper::booking).getContent(),
                page,
                size,
                result.getTotalElements(),
                result.getTotalPages());
    }


    private Booking lock(UUID id) {
        return bookings.lockById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
    }
}
