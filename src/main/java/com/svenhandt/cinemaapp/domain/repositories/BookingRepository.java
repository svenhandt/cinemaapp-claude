package com.svenhandt.cinemaapp.domain.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.svenhandt.cinemaapp.domain.entities.Booking;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT DISTINCT b FROM Booking b "
            + "JOIN FETCH b.screening s "
            + "JOIN FETCH s.movie "
            + "JOIN FETCH s.auditorium "
            + "JOIN FETCH b.seats "
            + "WHERE b.bookingNumber = :bookingNumber")
    Optional<Booking> findByBookingNumberWithScreeningAndSeats(@Param("bookingNumber") String bookingNumber);

    List<Booking> findByScreening(Screening screening);

    List<Booking> findByScreeningAndSeatsIn(Screening screening, Collection<Seat> seats);
}
