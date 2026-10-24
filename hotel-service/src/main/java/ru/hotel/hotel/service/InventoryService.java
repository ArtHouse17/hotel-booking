package ru.hotel.hotel.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hotel.common.*;
import ru.hotel.hotel.api.*;
import ru.hotel.hotel.api.HotelDtos.*;
import ru.hotel.hotel.domain.*;
import ru.hotel.hotel.domain.repository.*;

@Service
@Transactional(readOnly = true)
public class InventoryService {
    private final HotelRepository hotels;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final HotelMapper mapper;

    public InventoryService(
            HotelRepository hotels,
            RoomRepository rooms,
            ReservationRepository reservations,
            HotelMapper mapper) {
        this.hotels = hotels;
        this.rooms = rooms;
        this.reservations = reservations;
        this.mapper = mapper;
    }

    public PageDto<HotelView> hotels(String query, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw ApiException.badRequest("page >= 0; size must be 1..100");
        var all =
                hotels.findAll().stream()
                        .filter(
                                h ->
                                        query == null
                                                || (h.getName() + " " + h.getAddress())
                                                        .toLowerCase(Locale.ROOT)
                                                        .contains(query.toLowerCase(Locale.ROOT)))
                        .map(mapper::hotel)
                        .toList();
        int from = (int) Math.min((long) page * size, all.size());
        return new PageDto<>(
                all.subList(from, Math.min(from + size, all.size())),
                page,
                size,
                all.size(),
                (all.size() + size - 1) / size);
    }

    public HotelView hotel(Long id) {
        return mapper.hotel(findHotel(id));
    }

    @Transactional
    public HotelView createHotel(HotelInput input) {
        return mapper.hotel(hotels.save(new Hotel(input.name(), input.address())));
    }

    @Transactional
    public HotelView updateHotel(Long id, HotelInput input) {
        var hotel = findHotel(id);
        hotel.setName(input.name());
        hotel.setAddress(input.address());
        return mapper.hotel(hotels.save(hotel));
    }

    @Transactional
    public void deleteHotel(Long id) {
        var hotel = findHotel(id);
        if (rooms.existsByHotelId(id))
            throw ApiException.conflict("Delete the hotel's rooms first");
        hotels.delete(hotel);
    }

    public RoomView room(Long id) {
        return mapper.room(findRoom(id));
    }

    @Transactional
    public RoomView createRoom(RoomInput input) {
        return mapper.room(
                rooms.save(
                        new Room(findHotel(input.hotelId()), input.number(), input.available())));
    }

    @Transactional
    public RoomView updateRoom(Long id, RoomUpdate input) {
        var room = rooms.lockById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        room.setNumber(input.number());
        room.setAvailable(input.available());
        return mapper.room(rooms.save(room));
    }

    @Transactional
    public void deleteRoom(Long id) {
        var room = rooms.lockById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        if (reservations.existsByRoomId(id))
            throw ApiException.conflict("Room has booking history; set available=false instead");
        rooms.delete(room);
    }

    public List<RoomView> available(
            Long hotelId, LocalDate start, LocalDate end, boolean recommend) {
        DateRange.validate(start, end);
        var result = rooms.findAvailable(hotelId, start, end).stream().map(mapper::room);
        if (recommend)
            result =
                    result.sorted(
                            Comparator.comparingLong(RoomView::timesBooked)
                                    .thenComparing(RoomView::id));
        return result.toList();
    }

    public List<RoomStatistics> statistics(Long hotelId) {
        return rooms.findAll().stream()
                .filter(r -> hotelId == null || r.getHotel().getId().equals(hotelId))
                .map(
                        r ->
                                new RoomStatistics(
                                        r.getId(),
                                        r.getHotel().getId(),
                                        r.getNumber(),
                                        r.isAvailable(),
                                        r.getTimesBooked()))
                .toList();
    }

    private Hotel findHotel(Long id) {
        return hotels.findById(id).orElseThrow(() -> ApiException.notFound("Hotel not found"));
    }

    private Room findRoom(Long id) {
        return rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
    }
}
