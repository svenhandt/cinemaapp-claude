package com.svenhandt.cinemaapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.repositories.AuditoriumRepository;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.SeatDto;
import com.svenhandt.cinemaapp.exception.AuditoriumImportException;
import com.svenhandt.cinemaapp.mapper.AuditoriumMapper;

@ExtendWith(MockitoExtension.class)
class AuditoriumServiceTest {

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatService seatService;

    @Mock
    private AuditoriumMapper auditoriumMapper;

    @InjectMocks
    private AuditoriumService auditoriumService;

    @Test
    void importsAuditoriumWithItsSeats() {
        when(auditoriumRepository.save(any(Auditorium.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Auditorium auditorium = auditoriumService.importAuditorium("Saal 1", " X X\nXXX");

        assertThat(auditorium.getName()).isEqualTo("Saal 1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Seat>> seatsCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatRepository).saveAll(seatsCaptor.capture());
        List<Seat> seats = seatsCaptor.getValue();

        assertThat(seats).extracting(Seat::getRowNumber, Seat::getSeatNumber, Seat::getColumnPosition)
                .containsExactly(
                        tuple(1, 1, 1),
                        tuple(1, 2, 3),
                        tuple(2, 1, 0),
                        tuple(2, 2, 1),
                        tuple(2, 3, 2));
        assertThat(seats).allMatch(seat -> seat.getAuditorium() == auditorium);
    }

    @Test
    void rejectsEmptySeatingArrangement() {
        assertThatThrownBy(() -> auditoriumService.importAuditorium("Saal 1", ""))
                .isInstanceOf(AuditoriumImportException.class);

        verify(auditoriumRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    void rejectsInvalidCharacterInSeatingArrangement() {
        assertThatThrownBy(() -> auditoriumService.importAuditorium("Saal 1", "XXY"))
                .isInstanceOf(AuditoriumImportException.class);

        verify(auditoriumRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    void skipsImportWhenAuditoriumAlreadyExists() {
        Auditorium existingAuditorium = new Auditorium("Saal 1");
        when(auditoriumRepository.findByName("Saal 1")).thenReturn(Optional.of(existingAuditorium));

        Auditorium auditorium = auditoriumService.importAuditorium("Saal 1", "X");

        assertThat(auditorium).isSameAs(existingAuditorium);
        verify(auditoriumRepository, never()).save(any());
        verify(seatRepository, never()).saveAll(any());
    }

    @Test
    void returnsSeatsUnchangedWhenNoSeatIsBooked() {
        Auditorium auditorium = new Auditorium("Saal 1");
        List<SeatDto> seats = List.of(new SeatDto(1L, 1, 1, 0, false), new SeatDto(2L, 1, 2, 1, false));
        when(seatService.getSeats(auditorium)).thenReturn(seats);

        AuditoriumDto expected = new AuditoriumDto("Saal 1", seats);
        when(auditoriumMapper.toAuditoriumDto(auditorium, seats)).thenReturn(expected);

        AuditoriumDto result = auditoriumService.getAuditoriumDetails(auditorium, Set.of());

        assertThat(result).isSameAs(expected);
    }

    @Test
    void marksSeatsMatchingBookedSeatIdsAsBooked() {
        Auditorium auditorium = new Auditorium("Saal 1");
        SeatDto availableSeat = new SeatDto(1L, 1, 1, 0, false);
        SeatDto bookedSeat = new SeatDto(2L, 1, 2, 1, false);
        when(seatService.getSeats(auditorium)).thenReturn(List.of(availableSeat, bookedSeat));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SeatDto>> seatsCaptor = ArgumentCaptor.forClass(List.class);
        when(auditoriumMapper.toAuditoriumDto(eq(auditorium), seatsCaptor.capture()))
                .thenReturn(new AuditoriumDto("Saal 1", List.of()));

        auditoriumService.getAuditoriumDetails(auditorium, Set.of(2L));

        assertThat(seatsCaptor.getValue()).containsExactly(
                new SeatDto(1L, 1, 1, 0, false),
                new SeatDto(2L, 1, 2, 1, true));
    }

    @Test
    void returnsEmptyAuditoriumDtoWhenAuditoriumHasNoSeats() {
        Auditorium auditorium = new Auditorium("Saal 1");
        when(seatService.getSeats(auditorium)).thenReturn(List.of());

        AuditoriumDto expected = new AuditoriumDto("Saal 1", List.of());
        when(auditoriumMapper.toAuditoriumDto(auditorium, List.of())).thenReturn(expected);

        AuditoriumDto result = auditoriumService.getAuditoriumDetails(auditorium, Set.of());

        assertThat(result).isSameAs(expected);
    }
}
