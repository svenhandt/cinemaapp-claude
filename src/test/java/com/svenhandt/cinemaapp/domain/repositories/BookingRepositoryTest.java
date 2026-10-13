package com.svenhandt.cinemaapp.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Booking;
import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.entities.Weekday;

@DataJpaTest
class BookingRepositoryTest {

    private static final LocalDateTime BOOKING_DATE = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void findsBookingByBookingNumberWithScreeningAndSeatsEagerlyLoaded() {
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        Seat firstSeat = persistSeat(auditorium, 1, 1);
        Seat secondSeat = persistSeat(auditorium, 1, 2);
        Screening screening = persistScreening(auditorium, "Der Name der Rose", LocalTime.of(17, 0));
        entityManager.persistAndFlush(new Booking("B-1", screening, Set.of(firstSeat, secondSeat), BOOKING_DATE));
        entityManager.persistAndFlush(new Booking("B-2", screening, Set.of(persistSeat(auditorium, 2, 1)), BOOKING_DATE));
        entityManager.clear();

        Optional<Booking> found = bookingRepository.findByBookingNumberWithScreeningAndSeats("B-1");

        assertThat(found).isPresent();
        Booking booking = found.get();
        assertThat(booking.getBookingNumber()).isEqualTo("B-1");
        assertThat(Hibernate.isInitialized(booking.getScreening().getMovie())).isTrue();
        assertThat(Hibernate.isInitialized(booking.getScreening().getAuditorium())).isTrue();
        assertThat(Hibernate.isInitialized(booking.getSeats())).isTrue();
        assertThat(booking.getSeats()).containsExactlyInAnyOrder(firstSeat, secondSeat);
    }

    @Test
    void findsOnlyBookingsOfGivenScreening() {
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        Seat seat = persistSeat(auditorium, 1, 1);
        Screening screening = persistScreening(auditorium, "Der Name der Rose", LocalTime.of(17, 0));
        Screening otherScreening = persistScreening(auditorium, "Alien", LocalTime.of(20, 0));
        Booking booking = entityManager.persistAndFlush(new Booking("B-1", screening, Set.of(seat), BOOKING_DATE));
        entityManager.persistAndFlush(new Booking("B-2", otherScreening, Set.of(seat), BOOKING_DATE));
        entityManager.clear();

        List<Booking> bookings = bookingRepository.findByScreening(screening);

        assertThat(bookings).extracting(Booking::getId).containsExactly(booking.getId());
    }

    @Test
    void findsOnlyBookingsOfGivenScreeningContainingGivenSeats() {
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        Seat bookedSeat = persistSeat(auditorium, 1, 1);
        Seat otherSeat = persistSeat(auditorium, 1, 2);
        Screening screening = persistScreening(auditorium, "Der Name der Rose", LocalTime.of(17, 0));
        Screening otherScreening = persistScreening(auditorium, "Alien", LocalTime.of(20, 0));
        Booking matching = entityManager.persistAndFlush(new Booking("B-1", screening, Set.of(bookedSeat), BOOKING_DATE));
        entityManager.persistAndFlush(new Booking("B-2", screening, Set.of(otherSeat), BOOKING_DATE));
        entityManager.persistAndFlush(new Booking("B-3", otherScreening, Set.of(bookedSeat), BOOKING_DATE));
        entityManager.clear();

        List<Booking> bookings = bookingRepository.findByScreeningAndSeatsIn(screening, Set.of(bookedSeat));

        assertThat(bookings).extracting(Booking::getId).containsExactly(matching.getId());
    }

    private Seat persistSeat(Auditorium auditorium, int rowNumber, int seatNumber) {
        return entityManager.persistAndFlush(new Seat(auditorium, rowNumber, seatNumber, seatNumber));
    }

    private Screening persistScreening(Auditorium auditorium, String movieTitle, LocalTime startTime) {
        Movie movie = entityManager.persistAndFlush(new Movie(movieTitle));
        return entityManager.persistAndFlush(
                new Screening(movie, auditorium, Weekday.MO, startTime, new BigDecimal("7.00")));
    }
}
