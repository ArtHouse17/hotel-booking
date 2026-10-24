package ru.hotel.booking.api;

import org.mapstruct.*;
import ru.hotel.booking.api.BookingDtos.BookingView;
import ru.hotel.booking.api.UserDtos.UserView;
import ru.hotel.booking.domain.*;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    @Mapping(target = "userId", source = "user.id")
    BookingView booking(Booking booking);

    UserView user(User user);
}
