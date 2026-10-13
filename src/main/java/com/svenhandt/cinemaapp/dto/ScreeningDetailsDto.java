package com.svenhandt.cinemaapp.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.svenhandt.cinemaapp.domain.entities.Weekday;

public record ScreeningDetailsDto(Long id, String movieTitle, Weekday weekday, LocalTime startTime, BigDecimal price,
        AuditoriumDto auditorium) {
}
