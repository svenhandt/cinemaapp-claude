package com.svenhandt.cinemaapp.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Booking;
import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.entities.Weekday;
import com.svenhandt.cinemaapp.domain.repositories.AuditoriumRepository;
import com.svenhandt.cinemaapp.domain.repositories.BookingRepository;
import com.svenhandt.cinemaapp.domain.repositories.MovieRepository;
import com.svenhandt.cinemaapp.domain.repositories.ScreeningRepository;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.ScreeningDetailsDto;
import com.svenhandt.cinemaapp.dto.SeatDto;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScreeningDetailsIntegrationTest {

    private static final String MOVIE_TITLE = "Testfilm";
    private static final String AUDITORIUM_NAME = "Testsaal";
    private static final Weekday WEEKDAY = Weekday.FR;
    private static final LocalTime START_TIME = LocalTime.of(20, 0);
    private static final BigDecimal PRICE = new BigDecimal("9.50");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void screeningDetailsModelContainsAuditoriumWithThreeSeatsInTwoRowsAndOneBookedSeat() {
        Movie movie = movieRepository.save(new Movie(MOVIE_TITLE));
        Auditorium auditorium = auditoriumRepository.save(new Auditorium(AUDITORIUM_NAME));
        Seat bookedSeat = seatRepository.save(new Seat(auditorium, 1, 1, 0));
        seatRepository.save(new Seat(auditorium, 1, 2, 1));
        seatRepository.save(new Seat(auditorium, 2, 1, 0));
        Screening screening = screeningRepository.save(
                new Screening(movie, auditorium, WEEKDAY, START_TIME, PRICE));
        bookingRepository.save(new Booking("BK-TEST0001", screening, Set.of(bookedSeat), LocalDateTime.now()));

        assertThat(mvc.get().uri("/screenings/{screeningId}", screening.getId()))
                .hasStatusOk()
                .hasViewName("screening-details")
                .model()
                .extractingByKey("screening")
                .asInstanceOf(InstanceOfAssertFactories.type(ScreeningDetailsDto.class))
                .satisfies(screeningDetails -> {
                    assertThat(screeningDetails.id()).isEqualTo(screening.getId());
                    assertThat(screeningDetails.movieTitle()).isEqualTo(MOVIE_TITLE);
                    assertThat(screeningDetails.weekday()).isEqualTo(WEEKDAY);
                    assertThat(screeningDetails.startTime()).isEqualTo(START_TIME);
                    assertThat(screeningDetails.price()).isEqualByComparingTo(PRICE);

                    AuditoriumDto auditoriumDto = screeningDetails.auditorium();
                    assertThat(auditoriumDto.name()).isEqualTo(AUDITORIUM_NAME);
                    assertThat(auditoriumDto.seats())
                            .extracting(SeatDto::rowNumber, SeatDto::seatNumber, SeatDto::booked)
                            .containsExactlyInAnyOrder(
                                    tuple(1, 1, true),
                                    tuple(1, 2, false),
                                    tuple(2, 1, false));
                });
    }
}
