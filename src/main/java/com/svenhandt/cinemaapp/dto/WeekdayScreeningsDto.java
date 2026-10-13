package com.svenhandt.cinemaapp.dto;

import java.util.List;

import com.svenhandt.cinemaapp.domain.entities.Weekday;

public record WeekdayScreeningsDto(Weekday weekday, List<ScreeningDto> screenings) {
}
