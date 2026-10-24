package ru.hotel.hotel.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.hotel.hotel.domain.Hotel;
import ru.hotel.hotel.domain.repository.HotelRepository;
import ru.hotel.hotel.persistence.entity.HotelEntity;
import ru.hotel.hotel.persistence.repository.HotelJpaRepository;

import java.util.*;

@Repository
@Transactional(readOnly = true)
public class JpaHotelRepositoryAdapter implements HotelRepository {
    private final HotelJpaRepository jpa;
    private final HotelPersistenceMapper mapper;

    public JpaHotelRepositoryAdapter(HotelJpaRepository jpa, HotelPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public Optional<Hotel> findById(Long id) {
        return jpa.findById(id).map(mapper::domain);
    }

    public List<Hotel> findAll() {
        return jpa.findAll(Sort.by("id")).stream().map(mapper::domain).toList();
    }

    @Transactional
    public Hotel save(Hotel hotel) {
        var entity =
                hotel.getId() == null
                        ? new HotelEntity()
                        : jpa.findById(hotel.getId()).orElseGet(HotelEntity::new);
        mapper.update(hotel, entity);
        return mapper.domain(jpa.save(entity));
    }

    @Transactional
    public void delete(Hotel hotel) {
        jpa.deleteById(hotel.getId());
    }
}
