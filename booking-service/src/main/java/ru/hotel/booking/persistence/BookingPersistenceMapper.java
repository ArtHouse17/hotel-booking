package ru.hotel.booking.persistence;

import org.mapstruct.*;

import ru.hotel.booking.domain.*;
import ru.hotel.booking.persistence.entity.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookingPersistenceMapper {
    User domain(UserEntity entity);

    Booking domain(BookingEntity entity);

    void update(User user, @MappingTarget UserEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "user", ignore = true)
    void update(Booking booking, @MappingTarget BookingEntity entity);
}
