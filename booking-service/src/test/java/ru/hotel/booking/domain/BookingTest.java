package ru.hotel.booking.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ru.hotel.common.DomainConflictException;

class BookingTest {
    @Test
    void pendingBookingCanBeConfirmed() {
        var booking = new Booking();
        booking.selectRoom(1L);
        booking.confirm();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancelledBookingCannotBeConfirmedOrAssignedAnotherRoom() {
        var booking = new Booking();
        booking.selectRoom(1L);
        booking.cancel("Timeout", 503);
        assertThatThrownBy(booking::confirm).isInstanceOf(DomainConflictException.class);
        assertThatThrownBy(() -> booking.selectRoom(2L))
                .isInstanceOf(DomainConflictException.class);
        assertThat(booking.getRoomId()).isEqualTo(1L);
    }

    @Test
    void repeatingCancellationDoesNotRestartCompletedCompensation() {
        var booking = new Booking();
        booking.selectRoom(1L);
        booking.cancel("Timeout", 503);
        assertThat(booking.isCompensationRequired()).isTrue();
        booking.compensated();
        booking.cancel("Repeated cancellation", 409);
        assertThat(booking.isCompensationRequired()).isFalse();
        assertThat(booking.getFailureStatus()).isEqualTo(503);
    }
}
