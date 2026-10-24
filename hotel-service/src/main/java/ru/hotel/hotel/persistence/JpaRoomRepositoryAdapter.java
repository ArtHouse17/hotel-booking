package ru.hotel.hotel.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.hotel.hotel.domain.Room;
import ru.hotel.hotel.domain.repository.RoomRepository;
import ru.hotel.hotel.persistence.entity.RoomEntity;
import ru.hotel.hotel.persistence.repository.HotelJpaRepository;
import ru.hotel.hotel.persistence.repository.RoomJpaRepository;

import java.time.LocalDate;
import java.util.*;

@Repository
@Transactional(readOnly = true)
public class JpaRoomRepositoryAdapter implements RoomRepository {
    private final RoomJpaRepository jpa;
    private final HotelPersistenceMapper mapper;
    private final HotelJpaRepository hotels;

    public JpaRoomRepositoryAdapter(
            RoomJpaRepository jpa, HotelPersistenceMapper mapper, HotelJpaRepository hotels) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.hotels = hotels;
    }

    public Optional<Room> findById(Long id) {
        return jpa.findById(id).map(mapper::domain);
    }

    @Transactional
    public Optional<Room> lockById(Long id) {
        return jpa.lockById(id).map(mapper::domain);
    }

    public List<Room> findAll() {
        return jpa.findAll(Sort.by("timesBooked", "id")).stream().map(mapper::domain).toList();
    }

    public List<Room> findAvailable(Long hotelId, LocalDate start, LocalDate end) {
        return jpa.findAvailable(hotelId, start, end).stream().map(mapper::domain).toList();
    }

    public boolean existsByHotelId(Long hotelId) {
        return jpa.existsByHotelId(hotelId);
    }

    @Transactional
    public Room save(Room room) {
        var entity =
                room.getId() == null
                        ? new RoomEntity()
                        : jpa.findById(room.getId()).orElseGet(RoomEntity::new);
        mapper.update(room, entity);
        entity.setHotel(hotels.getReferenceById(room.getHotel().getId()));
        return mapper.domain(jpa.save(entity));
    }

    @Transactional
    public void delete(Room room) {
        jpa.deleteById(room.getId());
    }
}
