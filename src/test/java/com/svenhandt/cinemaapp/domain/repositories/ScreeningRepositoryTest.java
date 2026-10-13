package com.svenhandt.cinemaapp.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Weekday;

@DataJpaTest
class ScreeningRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Test
    void findsAllScreeningsWithMovieAndAuditoriumEagerlyLoaded() {
        Movie movie = entityManager.persistAndFlush(new Movie("Der Name der Rose"));
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        Screening screening = entityManager.persistAndFlush(
                new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), new BigDecimal("7.00")));
        entityManager.clear();

        List<Screening> screenings = screeningRepository.findAllWithMovieAndAuditorium();

        assertThat(screenings).hasSize(1);
        Screening found = screenings.get(0);
        assertThat(found.getId()).isEqualTo(screening.getId());
        assertThat(found.getMovie().getTitle()).isEqualTo("Der Name der Rose");
        assertThat(found.getAuditorium().getName()).isEqualTo("Saal 1");
    }

    @Test
    void findsScreeningByIdWithMovieAndAuditoriumEagerlyLoaded() {
        Movie movie = entityManager.persistAndFlush(new Movie("Der Name der Rose"));
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        Screening screening = entityManager.persistAndFlush(
                new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), new BigDecimal("7.00")));
        entityManager.clear();

        Optional<Screening> found = screeningRepository.findByIdWithMovieAndAuditorium(screening.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMovie().getTitle()).isEqualTo("Der Name der Rose");
        assertThat(found.get().getAuditorium().getName()).isEqualTo("Saal 1");
    }

    @Test
    void returnsEmptyWhenScreeningIdDoesNotExist() {
        Optional<Screening> found = screeningRepository.findByIdWithMovieAndAuditorium(999L);

        assertThat(found).isEmpty();
    }
}
