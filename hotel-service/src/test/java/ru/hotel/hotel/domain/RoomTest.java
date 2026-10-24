package ru.hotel.hotel.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.hotel.common.DomainConflictException;

class RoomTest {
    @Test
    void roomUnderMaintenanceCannotBeBooked() {
        var room = new Room(new Hotel("Hotel", "City"), "101", false);
        assertThatThrownBy(room::book).isInstanceOf(DomainConflictException.class);
        assertThat(room.getTimesBooked()).isZero();
    }

    @Test
    void repeatedReleaseDoesNotChangeCounterTwice() {
        var room = new Room(new Hotel("Hotel", "City"), "101", true);
        room.setId(1L);
        var slot = new RoomReservation(UUID.randomUUID(), room, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 3), true);
        room.book();
        if (slot.release()) room.release();
        if (slot.release()) room.release();
        assertThat(room.getTimesBooked()).isZero();
        assertThatThrownBy(slot::requireActive).isInstanceOf(DomainConflictException.class);
    }
}
