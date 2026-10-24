package ru.hotel.booking.persistence;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.booking.domain.Booking;
import ru.hotel.booking.domain.repository.BookingRepository;
import ru.hotel.booking.persistence.entity.BookingEntity;
import ru.hotel.booking.persistence.repository.BookingJpaRepository;
import ru.hotel.booking.persistence.repository.UserJpaRepository;
import ru.hotel.common.PageDto;

@Repository
@Transactional(readOnly = true)
public class JpaBookingRepositoryAdapter implements BookingRepository {
    private final BookingJpaRepository jpa;
    private final BookingPersistenceMapper mapper;
    private final UserJpaRepository users;

    public JpaBookingRepositoryAdapter(BookingJpaRepository jpa, BookingPersistenceMapper mapper, UserJpaRepository users) {
        this.jpa = jpa; this.mapper = mapper; this.users = users;
    }

    public Optional<Booking> findById(UUID id) { return jpa.findById(id).map(mapper::domain); }
    @Transactional
    public Optional<Booking> lockById(UUID id) { return jpa.lockById(id).map(mapper::domain); }
    public Optional<Booking> findByUserIdAndRequestId(Long userId, UUID requestId) {
        return jpa.findByUserIdAndRequestId(userId, requestId).map(mapper::domain);
    }
    public boolean existsByUserId(Long userId) { return jpa.existsByUserId(userId); }

    public PageDto<Booking> findByUserId(Long userId, int page, int size) {
        var result = jpa.findByUserId(userId, PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PageDto<>(result.map(mapper::domain).getContent(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public Booking save(Booking booking) {
        var entity = jpa.findById(booking.getId()).orElseGet(BookingEntity::new);
        mapper.update(booking, entity);
        entity.setUser(users.getReferenceById(booking.getUser().getId()));
        return mapper.domain(jpa.saveAndFlush(entity));
    }
}
