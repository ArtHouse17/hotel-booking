package ru.hotel.hotel.persistence;

import org.mapstruct.*;

import ru.hotel.hotel.domain.*;
import ru.hotel.hotel.persistence.entity.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HotelPersistenceMapper {
    Hotel domain(HotelEntity entity);

    Room domain(RoomEntity entity);

    RoomReservation domain(RoomReservationEntity entity);

    void update(Hotel hotel, @MappingTarget HotelEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    void update(Room room, @MappingTarget RoomEntity entity);

    @Mapping(target = "room", ignore = true)
    void update(RoomReservation reservation, @MappingTarget RoomReservationEntity entity);
}
