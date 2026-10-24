package ru.hotel.hotel.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.hotel.common.*;
import ru.hotel.hotel.api.HotelDtos.*;
import ru.hotel.hotel.service.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final InventoryService inventory;
    private final ReservationService reservations;

    public RoomController(InventoryService inventory, ReservationService reservations) {
        this.inventory = inventory;
        this.reservations = reservations;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomView> list(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long hotelId) {
        return inventory.available(hotelId, startDate, endDate, false);
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SERVICE')")
    public List<RoomView> recommend(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long hotelId) {
        return inventory.available(hotelId, startDate, endDate, true);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoomStatistics> statistics(
            @RequestParam(required = false) Long hotelId) {
        return inventory.statistics(hotelId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public RoomView get(@PathVariable Long id) {
        return inventory.room(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomView create(@Valid @RequestBody RoomInput input) {
        return inventory.createRoom(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomView update(@PathVariable Long id, @Valid @RequestBody RoomUpdate input) {
        return inventory.updateRoom(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        inventory.deleteRoom(id);
    }

    @PostMapping("/{id}/confirm-availability")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SERVICE')")
    public void confirm(@PathVariable Long id, @Valid @RequestBody ReservationCommand command) {
        reservations.confirm(id, command);
    }

    @PostMapping("/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SERVICE')")
    public void release(@PathVariable Long id, @Valid @RequestBody ReservationCommand command) {
        reservations.release(id, command);
    }
}
