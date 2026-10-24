package ru.hotel.booking.client;

import ru.hotel.common.*;

import java.time.LocalDate;
import java.util.List;

public interface HotelPort {
    List<RoomView> recommend(Long hotelId, LocalDate start, LocalDate end);

    void confirm(Long roomId, ReservationCommand command);

    void release(Long roomId, ReservationCommand command);
}
