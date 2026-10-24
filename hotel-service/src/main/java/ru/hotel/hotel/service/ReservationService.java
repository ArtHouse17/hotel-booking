package ru.hotel.hotel.service;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.common.*;
import ru.hotel.hotel.domain.*;
import ru.hotel.hotel.repository.*;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReservationService(RoomRepository rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
    }

    @Transactional
    public void confirm(Long roomId, ReservationCommand command) {
        DateRange.validate(command.startDate(), command.endDate());
        // The room row is the database mutex for all overlapping date intervals.
        var room = lock(roomId);
        var existing = reservations.findById(command.bookingId());
        if (existing.isPresent()) {
            validateSame(existing.get(), roomId, command);
            if (!existing.get().isActive())
                throw ApiException.conflict("Reservation has already been released");
            return;
        }
        if (!room.isAvailable()
                || reservations.overlaps(roomId, command.startDate(), command.endDate())) {
            throw ApiException.conflict("Room is unavailable for the requested dates");
        }
        reservations.save(
                new RoomReservation(
                        command.bookingId(), room, command.startDate(), command.endDate(), true));
        room.setTimesBooked(room.getTimesBooked() + 1);
        log.info("event=slot_confirmed bookingId={} roomId={}", command.bookingId(), roomId);
    }

    @Transactional
    public void release(Long roomId, ReservationCommand command) {
        DateRange.validate(command.startDate(), command.endDate());
        var room = lock(roomId);
        var existing = reservations.findById(command.bookingId());
        if (existing.isEmpty()) {
            // A delayed confirm after a timeout must never resurrect a cancelled booking.
            reservations.save(
                    new RoomReservation(
                            command.bookingId(),
                            room,
                            command.startDate(),
                            command.endDate(),
                            false));
        } else {
            var slot = existing.get();
            validateSame(slot, roomId, command);
            if (slot.isActive()) {
                slot.setActive(false);
                room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
            }
        }
        log.info("event=slot_released bookingId={} roomId={}", command.bookingId(), roomId);
    }

    private Room lock(Long id) {
        return rooms.lockById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
    }

    private void validateSame(RoomReservation slot, Long roomId, ReservationCommand command) {
        if (!slot.getRoom().getId().equals(roomId)
                || !slot.getStartDate().equals(command.startDate())
                || !slot.getEndDate().equals(command.endDate())) {
            throw ApiException.conflict(
                    "bookingId was already used with different reservation data");
        }
    }
}
