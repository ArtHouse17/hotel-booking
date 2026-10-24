package ru.hotel.common;

import java.time.LocalDate;

public final class DateRange {
    private DateRange() {}

    public static void validate(LocalDate start, LocalDate end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw ApiException.badRequest("endDate must be after startDate; dates are required");
        }
    }
}
