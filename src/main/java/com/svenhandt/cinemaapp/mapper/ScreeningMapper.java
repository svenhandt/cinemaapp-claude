package com.svenhandt.cinemaapp.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.MovieScreeningsDto;
import com.svenhandt.cinemaapp.dto.ScreeningDetailsDto;
import com.svenhandt.cinemaapp.dto.ScreeningDto;
import com.svenhandt.cinemaapp.dto.WeekdayScreeningsDto;

@Mapper(componentModel = "spring")
public interface ScreeningMapper {

    @Mapping(target = "auditoriumName", source = "auditorium.name")
    ScreeningDto toScreeningDto(Screening screening);

    List<ScreeningDto> toScreeningDtoList(List<Screening> screenings);

    @Mapping(target = "movieId", source = "movie.id")
    @Mapping(target = "movieTitle", source = "movie.title")
    @Mapping(target = "weekdaySchedule", source = "weekdaySchedule")
    MovieScreeningsDto toMovieScreeningsDto(Movie movie, List<WeekdayScreeningsDto> weekdaySchedule);

    @Mapping(target = "movieTitle", source = "screening.movie.title")
    @Mapping(target = "auditorium", source = "auditoriumDto")
    ScreeningDetailsDto toScreeningDetailsDto(Screening screening, AuditoriumDto auditoriumDto);
}
