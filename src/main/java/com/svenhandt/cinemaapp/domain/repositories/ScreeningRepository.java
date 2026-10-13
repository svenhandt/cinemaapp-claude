package com.svenhandt.cinemaapp.domain.repositories;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Weekday;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    List<Screening> findByMovie(Movie movie);

    Optional<Screening> findByMovieAndWeekdayAndStartTime(Movie movie, Weekday weekday, LocalTime startTime);

    @Query("SELECT s FROM Screening s JOIN FETCH s.movie JOIN FETCH s.auditorium")
    List<Screening> findAllWithMovieAndAuditorium();

    @Query("SELECT s FROM Screening s JOIN FETCH s.movie JOIN FETCH s.auditorium WHERE s.id = :id")
    Optional<Screening> findByIdWithMovieAndAuditorium(@Param("id") Long id);
}
