package com.svenhandt.cinemaapp.dto;

public record SeatDto(Long id, int rowNumber, int seatNumber, int columnPosition, boolean booked) {
}
