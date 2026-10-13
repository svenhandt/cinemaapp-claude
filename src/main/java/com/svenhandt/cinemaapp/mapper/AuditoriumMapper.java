package com.svenhandt.cinemaapp.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.SeatDto;

@Mapper(componentModel = "spring")
public interface AuditoriumMapper {

    AuditoriumDto toAuditoriumDto(Auditorium auditorium, List<SeatDto> seats);
}
