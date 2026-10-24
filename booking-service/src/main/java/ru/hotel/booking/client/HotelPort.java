package ru.hotel.booking.client;

import java.time.LocalDate;
import java.util.List;
import ru.hotel.common.*;

public interface HotelPort {
    List<RoomView> recommend(Long hotelId, LocalDate start, LocalDate end);

    void confirm(Long roomId, ReservationCommand command);

    void release(Long roomId, ReservationCommand command);
}
