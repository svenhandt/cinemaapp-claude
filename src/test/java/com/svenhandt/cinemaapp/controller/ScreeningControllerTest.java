package com.svenhandt.cinemaapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.svenhandt.cinemaapp.domain.entities.Weekday;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.MovieScreeningsDto;
import com.svenhandt.cinemaapp.dto.ScreeningDetailsDto;
import com.svenhandt.cinemaapp.dto.ScreeningDto;
import com.svenhandt.cinemaapp.dto.SeatDto;
import com.svenhandt.cinemaapp.dto.WeekdayScreeningsDto;
import com.svenhandt.cinemaapp.exception.ScreeningNotFoundException;
import com.svenhandt.cinemaapp.service.ScreeningService;

@WebMvcTest(ScreeningController.class)
class ScreeningControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ScreeningService screeningService;

    @Test
    void showsScreeningsGroupedByMovie() {
        List<MovieScreeningsDto> overview = List.of(new MovieScreeningsDto(1L, "Der Name der Rose",
                List.of(new WeekdayScreeningsDto(Weekday.MO, List.of(
                        new ScreeningDto(1L, Weekday.MO, LocalTime.of(17, 0), "Saal 1", new BigDecimal("7.00")))))));
        when(screeningService.findScreeningsOverview()).thenReturn(overview);

        assertThat(mvc.get().uri("/"))
                .hasStatusOk()
                .hasViewName("screenings-overview")
                .model().containsEntry("movieScreenings", overview);
    }

    @Test
    void showsNoScreeningsMessageWhenNoneExist() {
        when(screeningService.findScreeningsOverview()).thenReturn(List.of());

        assertThat(mvc.get().uri("/"))
                .hasStatusOk()
                .hasViewName("screenings-overview")
                .model().containsEntry("movieScreenings", List.of());
    }

    @Test
    void showsScreeningDetailsForSelectedScreening() {
        AuditoriumDto auditoriumDto = new AuditoriumDto("Saal 1", List.of(new SeatDto(1L, 1, 1, 0, false)));
        ScreeningDetailsDto screeningDetails = new ScreeningDetailsDto(1L, "Der Name der Rose", Weekday.MO,
                LocalTime.of(17, 0), new BigDecimal("7.00"), auditoriumDto);
        when(screeningService.getScreeningDetails(1L)).thenReturn(screeningDetails);

        assertThat(mvc.get().uri("/screenings/{id}", 1L))
                .hasStatusOk()
                .hasViewName("screening-details")
                .model().containsEntry("screening", screeningDetails);
    }

    @Test
    void returnsNotFoundWhenScreeningDoesNotExist() {
        when(screeningService.getScreeningDetails(99L)).thenThrow(new ScreeningNotFoundException());

        assertThat(mvc.get().uri("/screenings/{id}", 99L)).hasStatus(HttpStatus.NOT_FOUND);
    }
}
