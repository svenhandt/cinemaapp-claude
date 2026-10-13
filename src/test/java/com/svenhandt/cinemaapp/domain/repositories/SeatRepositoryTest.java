package com.svenhandt.cinemaapp.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Seat;

@DataJpaTest
class SeatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    void ordersSeatsByRowNumberThenColumnPosition() {
        Auditorium auditorium = entityManager.persist(new Auditorium("Saal 1"));
        entityManager.persist(new Seat(auditorium, 2, 1, 2));
        entityManager.persist(new Seat(auditorium, 1, 1, 3));
        entityManager.persist(new Seat(auditorium, 2, 2, 1));
        entityManager.persist(new Seat(auditorium, 1, 2, 1));
        entityManager.persist(new Seat(auditorium, 1, 3, 2));
        entityManager.flush();
        entityManager.clear();

        List<Seat> seats = seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(auditorium);

        assertThat(seats)
                .extracting(Seat::getRowNumber, Seat::getColumnPosition)
                .containsExactly(
                        tuple(1, 1),
                        tuple(1, 2),
                        tuple(1, 3),
                        tuple(2, 1),
                        tuple(2, 2));
    }

    @Test
    void returnsOnlySeatsOfGivenAuditorium() {
        Auditorium auditorium = entityManager.persist(new Auditorium("Saal 1"));
        Auditorium otherAuditorium = entityManager.persist(new Auditorium("Saal 2"));
        entityManager.persist(new Seat(auditorium, 1, 1, 1));
        entityManager.persist(new Seat(otherAuditorium, 1, 1, 1));
        entityManager.flush();
        entityManager.clear();

        List<Seat> seats = seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(auditorium);

        assertThat(seats).hasSize(1);
    }

    @Test
    void returnsEmptyListWhenAuditoriumHasNoSeats() {
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        entityManager.clear();

        List<Seat> seats = seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(auditorium);

        assertThat(seats).isEmpty();
    }
}
