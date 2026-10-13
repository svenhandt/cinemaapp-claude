package com.svenhandt.cinemaapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.svenhandt.cinemaapp.domain.entities.Weekday;

public record BookingConfirmationDto(String bookingNumber, String movieTitle, Weekday weekday, LocalTime startTime,
        String auditoriumName, List<BookingSeatDto> seats, BigDecimal pricePerSeat, BigDecimal total,
        LocalDateTime bookingDate) {
}
