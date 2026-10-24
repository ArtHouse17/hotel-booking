package ru.hotel.hotel.domain.repository;

import java.util.*;
import ru.hotel.hotel.domain.Hotel;

public interface HotelRepository {
    Optional<Hotel> findById(Long id);
    List<Hotel> findAll();
    Hotel save(Hotel hotel);
    void delete(Hotel hotel);
    long count();
}
