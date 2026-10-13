package com.svenhandt.cinemaapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;
import com.svenhandt.cinemaapp.dto.SeatDto;
import com.svenhandt.cinemaapp.mapper.SeatMapper;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatMapper seatMapper;

    @InjectMocks
    private SeatService seatService;

    @Test
    void returnsSeatsOfAuditoriumMappedToDtos() {
        Auditorium auditorium = new Auditorium("Saal 1");
        Seat seat1 = new Seat(auditorium, 1, 1, 0);
        Seat seat2 = new Seat(auditorium, 1, 2, 1);
        when(seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(auditorium))
                .thenReturn(List.of(seat1, seat2));

        List<SeatDto> expected = List.of(new SeatDto(null, 1, 1, 0, false), new SeatDto(null, 1, 2, 1, false));
        when(seatMapper.toSeatDtoList(List.of(seat1, seat2))).thenReturn(expected);

        List<SeatDto> result = seatService.getSeats(auditorium);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void returnsEmptyListWhenAuditoriumHasNoSeats() {
        Auditorium auditorium = new Auditorium("Saal 1");
        when(seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(auditorium)).thenReturn(List.of());
        when(seatMapper.toSeatDtoList(List.of())).thenReturn(List.of());

        List<SeatDto> result = seatService.getSeats(auditorium);

        assertThat(result).isEmpty();
    }
}
