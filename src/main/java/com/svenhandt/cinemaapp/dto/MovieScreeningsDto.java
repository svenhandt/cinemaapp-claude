package com.svenhandt.cinemaapp.dto;

import java.util.List;

public record MovieScreeningsDto(Long movieId, String movieTitle, List<WeekdayScreeningsDto> weekdaySchedule) {
}
