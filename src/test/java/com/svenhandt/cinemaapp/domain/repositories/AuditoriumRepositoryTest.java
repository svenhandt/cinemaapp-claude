package com.svenhandt.cinemaapp.domain.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;

@DataJpaTest
class AuditoriumRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Test
    void findsAuditoriumByName() {
        Auditorium auditorium = entityManager.persistAndFlush(new Auditorium("Saal 1"));
        entityManager.persistAndFlush(new Auditorium("Saal 2"));
        entityManager.clear();

        Optional<Auditorium> found = auditoriumRepository.findByName("Saal 1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auditorium.getId());
        assertThat(found.get().getName()).isEqualTo("Saal 1");
    }
}
