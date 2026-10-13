package com.svenhandt.cinemaapp.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.svenhandt.cinemaapp.domain.entities.Booking;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.dto.BookingConfirmationDto;
import com.svenhandt.cinemaapp.dto.BookingInitDto;
import com.svenhandt.cinemaapp.dto.BookingSeatDto;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "screeningId", source = "screening.id")
    @Mapping(target = "movieTitle", source = "screening.movie.title")
    @Mapping(target = "auditoriumName", source = "screening.auditorium.name")
    @Mapping(target = "weekday", source = "screening.weekday")
    @Mapping(target = "startTime", source = "screening.startTime")
    BookingInitDto toBookingInitDto(Screening screening, List<BookingSeatDto> seats, BigDecimal total,
            int currentMonth, int currentYear);

    @Mapping(target = "bookingNumber", source = "booking.bookingNumber")
    @Mapping(target = "movieTitle", source = "booking.screening.movie.title")
    @Mapping(target = "auditoriumName", source = "booking.screening.auditorium.name")
    @Mapping(target = "weekday", source = "booking.screening.weekday")
    @Mapping(target = "startTime", source = "booking.screening.startTime")
    @Mapping(target = "bookingDate", source = "booking.bookingDate")
    BookingConfirmationDto toBookingConfirmationDto(Booking booking, List<BookingSeatDto> seats,
            BigDecimal pricePerSeat, BigDecimal total);

    BookingSeatDto toBookingSeatDto(Seat seat);

    List<BookingSeatDto> toBookingSeatDtoList(List<Seat> seats);
}
