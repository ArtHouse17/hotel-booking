package ru.hotel.hotel.service;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.hotel.common.*;
import ru.hotel.hotel.domain.*;
import ru.hotel.hotel.domain.repository.*;

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
            existing.get().verifyData(roomId, command.startDate(), command.endDate());
            existing.get().requireActive();
            return;
        }
        if (!room.isAvailable()
                || reservations.overlaps(roomId, command.startDate(), command.endDate())) {
            throw ApiException.conflict("Room is unavailable for the requested dates");
        }
        reservations.save(
                new RoomReservation(
                        command.bookingId(), room, command.startDate(), command.endDate(), true));
        room.book();
        rooms.save(room);
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
            slot.verifyData(roomId, command.startDate(), command.endDate());
            if (slot.release()) {
                room.release();
                reservations.save(slot);
                rooms.save(room);
            }
        }
        log.info("event=slot_released bookingId={} roomId={}", command.bookingId(), roomId);
    }

    private Room lock(Long id) {
        return rooms.lockById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
    }
}
