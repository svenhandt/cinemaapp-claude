package com.svenhandt.cinemaapp.dto;

import java.util.List;

public record AuditoriumDto(String name, List<SeatDto> seats) {
}
