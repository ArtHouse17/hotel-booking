package ru.hotel.booking.service;

import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import ru.hotel.booking.api.BookingDtos.*;
import ru.hotel.booking.client.*;
import ru.hotel.common.*;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingTransactions transactions;
    private final HotelPort hotel;

    public BookingService(BookingTransactions transactions, HotelPort hotel) {
        this.transactions = transactions;
        this.hotel = hotel;
    }

    public BookingOutcome create(Long userId, CreateBooking input) {
        var begun = transactions.begin(userId, input);
        if (!begun.created()) return begun;
        var b = begun.booking();
        try {
            if (b.autoSelect()) autoConfirm(b);
            else hotel.confirm(b.roomId(), command(b));
            return new BookingOutcome(transactions.confirm(b.id()), true);
        } catch (RuntimeException e) {
            int status = 503;
            if (e instanceof HotelCallException remote
                    && Set.of(400, 404, 409).contains(remote.status())) {
                status = remote.status();
            }
            if (e instanceof ApiException local) status = local.status().value();
            log.warn("event=booking_failed bookingId={} reason={}", b.id(), e.getMessage());
            var cancelled =
                    transactions.cancel(
                            b.id(),
                            status == 409
                                    ? "Room unavailable or booking cancelled"
                                    : "Confirmation failed; booking cancelled",
                            status);
            compensate(cancelled);
            return new BookingOutcome(transactions.internal(b.id()), true);
        }
    }

    private void autoConfirm(BookingView booking) {
        // The Hotel lock is the final arbiter: a recommendation is only a snapshot.
        var candidates = recommend(booking.hotelId(), booking.startDate(), booking.endDate());
        for (var room : candidates) {
            var selected = transactions.selectRoom(booking.id(), room.id());
            try {
                hotel.confirm(room.id(), command(selected));
                return;
            } catch (HotelCallException e) {
                if (e.status() != 409) throw e;
                log.info(
                        "event=auto_selection_conflict bookingId={} roomId={}",
                        booking.id(),
                        room.id());
            }
        }
        throw ApiException.conflict("No rooms available for the requested dates");
    }

    public List<RoomView> recommend(Long hotelId, LocalDate start, LocalDate end) {
        DateRange.validate(start, end);
        try {
            return hotel.recommend(hotelId, start, end).stream()
                    .sorted(
                            Comparator.comparingLong(RoomView::timesBooked)
                                    .thenComparing(RoomView::id))
                    .toList();
        } catch (HotelCallException e) {
            throw new ApiException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Hotel is temporarily unavailable");
        }
    }

    public BookingView cancel(Long userId, UUID id) {
        transactions.get(id, userId);
        var cancelled = transactions.cancel(id, "Cancelled by user", 409);
        compensate(cancelled);
        return transactions.internal(id);
    }

    public void compensate(BookingView booking) {
        if (!booking.compensationRequired() || booking.roomId() == null) return;
        try {
            hotel.release(booking.roomId(), command(booking));
            transactions.compensated(booking.id());
        } catch (HotelCallException e) {
            if (e.status() == 404) transactions.compensated(booking.id());
            else
                log.warn(
                        "event=compensation_pending bookingId={} reason={}",
                        booking.id(),
                        e.getMessage());
        } catch (RuntimeException e) {
            log.error("event=compensation_pending bookingId={}", booking.id(), e);
        }
    }

    private ReservationCommand command(BookingView booking) {
        return new ReservationCommand(booking.id(), booking.startDate(), booking.endDate());
    }
}
