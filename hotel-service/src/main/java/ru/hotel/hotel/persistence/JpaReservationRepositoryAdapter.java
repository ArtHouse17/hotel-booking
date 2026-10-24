package ru.hotel.hotel.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.hotel.hotel.domain.RoomReservation;
import ru.hotel.hotel.domain.repository.ReservationRepository;
import ru.hotel.hotel.persistence.entity.RoomReservationEntity;
import ru.hotel.hotel.persistence.repository.ReservationJpaRepository;
import ru.hotel.hotel.persistence.repository.RoomJpaRepository;

import java.time.LocalDate;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class JpaReservationRepositoryAdapter implements ReservationRepository {
    private final ReservationJpaRepository jpa;
    private final HotelPersistenceMapper mapper;
    private final RoomJpaRepository rooms;

    public JpaReservationRepositoryAdapter(
            ReservationJpaRepository jpa, HotelPersistenceMapper mapper, RoomJpaRepository rooms) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.rooms = rooms;
    }

    public Optional<RoomReservation> findById(UUID id) {
        return jpa.findById(id).map(mapper::domain);
    }

    public boolean overlaps(Long roomId, LocalDate start, LocalDate end) {
        return jpa.overlaps(roomId, start, end);
    }

    public boolean existsByRoomId(Long roomId) {
        return jpa.existsByRoomId(roomId);
    }

    @Transactional
    public RoomReservation save(RoomReservation reservation) {
        var entity = jpa.findById(reservation.getBookingId()).orElseGet(RoomReservationEntity::new);
        mapper.update(reservation, entity);
        entity.setRoom(rooms.getReferenceById(reservation.getRoom().getId()));
        return mapper.domain(jpa.save(entity));
    }
}
