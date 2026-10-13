package com.svenhandt.cinemaapp.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.svenhandt.cinemaapp.domain.entities.Movie;

@DataJpaTest
class MovieRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void findsMovieByTitle() {
        Movie movie = entityManager.persistAndFlush(new Movie("Inception"));
        entityManager.persistAndFlush(new Movie("Interstellar"));
        entityManager.clear();

        Optional<Movie> found = movieRepository.findByTitle("Inception");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(movie.getId());
        assertThat(found.get().getTitle()).isEqualTo("Inception");
    }

    @Test
    void returnsEmptyWhenNoMovieHasTitle() {
        entityManager.persistAndFlush(new Movie("Inception"));
        entityManager.clear();

        Optional<Movie> found = movieRepository.findByTitle("Unknown Movie");

        assertThat(found).isEmpty();
    }
}
