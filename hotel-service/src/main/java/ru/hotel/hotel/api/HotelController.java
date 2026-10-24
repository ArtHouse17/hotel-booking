package ru.hotel.hotel.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.hotel.common.PageDto;
import ru.hotel.hotel.api.HotelDtos.*;
import ru.hotel.hotel.service.InventoryService;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    private final InventoryService inventory;

    public HotelController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageDto<HotelView> list(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventory.hotels(query, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public HotelView get(@PathVariable Long id) {
        return inventory.hotel(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public HotelView create(@Valid @RequestBody HotelInput input) {
        return inventory.createHotel(input);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelView update(@PathVariable Long id, @Valid @RequestBody HotelInput input) {
        return inventory.updateHotel(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        inventory.deleteHotel(id);
    }
}
