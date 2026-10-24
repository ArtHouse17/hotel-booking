package ru.hotel.hotel.api;

import org.mapstruct.*;
import ru.hotel.common.RoomView;
import ru.hotel.hotel.api.HotelDtos.HotelView;
import ru.hotel.hotel.domain.*;

@Mapper(componentModel = "spring")
public interface HotelMapper {
    HotelView hotel(Hotel hotel);

    @Mapping(target = "hotelId", source = "hotel.id")
    RoomView room(Room room);
}
