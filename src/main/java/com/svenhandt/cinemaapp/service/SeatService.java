package com.svenhandt.cinemaapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;
import com.svenhandt.cinemaapp.dto.SeatDto;
import com.svenhandt.cinemaapp.mapper.SeatMapper;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    public SeatService(SeatRepository seatRepository, SeatMapper seatMapper) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
    }

    @Transactional(readOnly = true)
    public List<SeatDto> getSeats(Auditorium auditorium) {
        return seatMapper.toSeatDtoList(seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(auditorium));
    }
}
