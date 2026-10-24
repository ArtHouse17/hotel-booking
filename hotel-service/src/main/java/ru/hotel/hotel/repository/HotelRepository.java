package ru.hotel.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hotel.hotel.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}
