package com.svenhandt.cinemaapp.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.svenhandt.cinemaapp.domain.entities.Weekday;

public record ScreeningDto(Long id, Weekday weekday, LocalTime startTime, String auditoriumName, BigDecimal price) {
}
