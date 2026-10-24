package ru.hotel.hotel.domain.repository;

import ru.hotel.hotel.domain.Hotel;

import java.util.*;

public interface HotelRepository {
    Optional<Hotel> findById(Long id);

    List<Hotel> findAll();

    Hotel save(Hotel hotel);

    void delete(Hotel hotel);
}
