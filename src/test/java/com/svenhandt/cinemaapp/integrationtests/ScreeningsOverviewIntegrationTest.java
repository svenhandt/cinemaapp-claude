package com.svenhandt.cinemaapp.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Weekday;
import com.svenhandt.cinemaapp.domain.repositories.AuditoriumRepository;
import com.svenhandt.cinemaapp.domain.repositories.MovieRepository;
import com.svenhandt.cinemaapp.domain.repositories.ScreeningRepository;
import com.svenhandt.cinemaapp.dto.MovieScreeningsDto;
import com.svenhandt.cinemaapp.dto.ScreeningDto;
import com.svenhandt.cinemaapp.dto.WeekdayScreeningsDto;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScreeningsOverviewIntegrationTest {

    private static final String MOVIE_TITLE = "Testfilm";
    private static final String AUDITORIUM_NAME = "Testsaal";
    private static final LocalTime START_TIME = LocalTime.of(20, 0);
    private static final BigDecimal PRICE = new BigDecimal("9.50");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Test
    void screeningsOverviewModelContainsOneScreeningPerWeekday() {
        Movie movie = movieRepository.save(new Movie(MOVIE_TITLE));
        Auditorium auditorium = auditoriumRepository.save(new Auditorium(AUDITORIUM_NAME));
        Arrays.stream(Weekday.values())
                .forEach(weekday -> screeningRepository.save(
                        new Screening(movie, auditorium, weekday, START_TIME, PRICE)));

        assertThat(mvc.get().uri("/"))
                .hasStatusOk()
                .hasViewName("screenings-overview")
                .model()
                .extractingByKey("movieScreenings")
                .asInstanceOf(InstanceOfAssertFactories.list(MovieScreeningsDto.class))
                .singleElement()
                .satisfies(movieScreenings -> {
                    assertThat(movieScreenings.movieTitle()).isEqualTo(MOVIE_TITLE);

                    List<WeekdayScreeningsDto> weekdaySchedule = movieScreenings.weekdaySchedule();
                    assertThat(weekdaySchedule)
                            .extracting(WeekdayScreeningsDto::weekday)
                            .containsExactlyInAnyOrder(Weekday.values());
                    assertThat(weekdaySchedule).allSatisfy(weekdayScreenings ->
                            assertThat(weekdayScreenings.screenings())
                                    .singleElement()
                                    .returns(weekdayScreenings.weekday(), ScreeningDto::weekday)
                                    .returns(START_TIME, ScreeningDto::startTime)
                                    .returns(AUDITORIUM_NAME, ScreeningDto::auditoriumName)
                                    .returns(PRICE, ScreeningDto::price));
                });
    }
}
