package ru.hotel.hotel.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.hotel.hotel.persistence.entity.HotelEntity;

public interface HotelJpaRepository extends JpaRepository<HotelEntity, Long> {}
