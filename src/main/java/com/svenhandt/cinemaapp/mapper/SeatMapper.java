package com.svenhandt.cinemaapp.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.dto.SeatDto;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    @Mapping(target = "booked", constant = "false")
    SeatDto toSeatDto(Seat seat);

    List<SeatDto> toSeatDtoList(List<Seat> seats);
}
