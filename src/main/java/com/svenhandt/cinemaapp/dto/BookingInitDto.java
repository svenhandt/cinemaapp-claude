package com.svenhandt.cinemaapp.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.svenhandt.cinemaapp.domain.entities.Weekday;

public record BookingInitDto(Long screeningId, String movieTitle, Weekday weekday, LocalTime startTime,
        String auditoriumName, List<BookingSeatDto> seats, BigDecimal total, int currentMonth, int currentYear) {
}
