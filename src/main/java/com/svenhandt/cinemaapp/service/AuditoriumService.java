package com.svenhandt.cinemaapp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.repositories.AuditoriumRepository;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.SeatDto;
import com.svenhandt.cinemaapp.exception.AuditoriumImportException;
import com.svenhandt.cinemaapp.mapper.AuditoriumMapper;

@Service
public class AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final SeatService seatService;
    private final AuditoriumMapper auditoriumMapper;

    public AuditoriumService(AuditoriumRepository auditoriumRepository, SeatRepository seatRepository,
            SeatService seatService, AuditoriumMapper auditoriumMapper) {
        this.auditoriumRepository = auditoriumRepository;
        this.seatRepository = seatRepository;
        this.seatService = seatService;
        this.auditoriumMapper = auditoriumMapper;
    }

    @Transactional(readOnly = true)
    public AuditoriumDto getAuditoriumDetails(Auditorium auditorium, Set<Long> bookedSeatIds) {
        List<SeatDto> seats = withBookedSeats(seatService.getSeats(auditorium), bookedSeatIds);
        return auditoriumMapper.toAuditoriumDto(auditorium, seats);
    }

    private List<SeatDto> withBookedSeats(List<SeatDto> seats, Set<Long> bookedSeatIds) {
        if (bookedSeatIds.isEmpty()) {
            return seats;
        }
        return seats.stream()
                .map(seat -> bookedSeatIds.contains(seat.id())
                        ? new SeatDto(seat.id(), seat.rowNumber(), seat.seatNumber(), seat.columnPosition(), true)
                        : seat)
                .toList();
    }

    @Transactional
    public Auditorium importAuditorium(String auditoriumName, String seatingArrangement) {
        return auditoriumRepository.findByName(auditoriumName)
                .orElseGet(() -> createAuditorium(auditoriumName, seatingArrangement));
    }

    private Auditorium createAuditorium(String auditoriumName, String seatingArrangement) {
        if (StringUtils.isBlank(seatingArrangement)) {
            throw new AuditoriumImportException(
                    "The auditorium file for '" + auditoriumName + "' is empty.");
        }

        List<SeatPosition> seatPositions = parseSeatPositions(auditoriumName, seatingArrangement);

        if (CollectionUtils.isEmpty(seatPositions)) {
            throw new AuditoriumImportException(
                    "The auditorium file for '" + auditoriumName + "' contains no seats.");
        }

        Auditorium auditorium = auditoriumRepository.save(new Auditorium(auditoriumName));

        List<Seat> seats = new ArrayList<>();
        for (SeatPosition seatPosition : seatPositions) {
            seats.add(new Seat(auditorium, seatPosition.rowNumber(), seatPosition.seatNumber(),
                    seatPosition.columnPosition()));
        }
        seatRepository.saveAll(seats);

        return auditorium;
    }

    private List<SeatPosition> parseSeatPositions(String auditoriumName, String seatingArrangement) {
        List<SeatPosition> seatPositions = new ArrayList<>();
        List<String> rows = seatingArrangement.lines().toList();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String row = rows.get(rowIndex);
            int rowNumber = rowIndex + 1;
            int seatNumber = 0;

            for (int columnPosition = 0; columnPosition < row.length(); columnPosition++) {
                char character = row.charAt(columnPosition);
                if (character == 'X') {
                    seatNumber++;
                    seatPositions.add(new SeatPosition(rowNumber, seatNumber, columnPosition));
                } else if (character != ' ') {
                    throw new AuditoriumImportException(
                            "The auditorium file for '" + auditoriumName
                                    + "' contains an invalid character '" + character + "' in row " + rowNumber
                                    + ".");
                }
            }
        }

        return seatPositions;
    }

    private record SeatPosition(int rowNumber, int seatNumber, int columnPosition) {
    }
}
