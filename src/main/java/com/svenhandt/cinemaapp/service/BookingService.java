package com.svenhandt.cinemaapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svenhandt.cinemaapp.domain.entities.Booking;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.repositories.BookingRepository;
import com.svenhandt.cinemaapp.domain.repositories.ScreeningRepository;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;
import com.svenhandt.cinemaapp.dto.BookingConfirmationDto;
import com.svenhandt.cinemaapp.dto.BookingInitDto;
import com.svenhandt.cinemaapp.dto.BookingSeatDto;
import com.svenhandt.cinemaapp.dto.PaymentDto;
import com.svenhandt.cinemaapp.exception.BookingNotFoundException;
import com.svenhandt.cinemaapp.exception.BookingValidationException;
import com.svenhandt.cinemaapp.exception.InvalidCardNumberException;
import com.svenhandt.cinemaapp.exception.ScreeningNotFoundException;
import com.svenhandt.cinemaapp.mapper.BookingMapper;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository, ScreeningRepository screeningRepository,
            SeatRepository seatRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.screeningRepository = screeningRepository;
        this.seatRepository = seatRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public List<Booking> findByScreening(Screening screening) {
        return bookingRepository.findByScreening(screening);
    }

    @Transactional(readOnly = true)
    public BookingInitDto initBooking(Long screeningId, List<Long> seatIds) {
        Screening screening = screeningRepository.findByIdWithMovieAndAuditorium(screeningId)
                .orElseThrow(ScreeningNotFoundException::new);

        if (CollectionUtils.isEmpty(seatIds)) {
            throw new BookingValidationException("Bitte wählen Sie mindestens einen Sitzplatz aus.");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        boolean someSeatUnknown = seats.size() != new LinkedHashSet<>(seatIds).size();
        boolean someSeatAlreadyBooked =
                CollectionUtils.isNotEmpty(bookingRepository.findByScreeningAndSeatsIn(screening, seats));
        if (someSeatUnknown || someSeatAlreadyBooked) {
            throw new BookingValidationException("Der Sitzplatz ist nicht mehr verfügbar.");
        }

        BigDecimal total = calculateTotal(screening.getPrice(), seats.size());
        List<BookingSeatDto> seatDtos = bookingMapper.toBookingSeatDtoList(seats);
        LocalDate today = LocalDate.now();
        return bookingMapper.toBookingInitDto(screening, seatDtos, total, today.getMonthValue(), today.getYear());
    }

    @Transactional
    public BookingConfirmationDto completeBooking(Long screeningId, List<Long> seatIds, PaymentDto payment) {
        Screening screening = screeningRepository.findByIdWithMovieAndAuditorium(screeningId)
                .orElseThrow(ScreeningNotFoundException::new);

        if (CollectionUtils.isEmpty(seatIds)) {
            throw new BookingValidationException("Bitte wählen Sie mindestens einen Sitzplatz aus.");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        boolean someSeatUnknown = seats.size() != new LinkedHashSet<>(seatIds).size();
        boolean someSeatAlreadyBooked =
                CollectionUtils.isNotEmpty(bookingRepository.findByScreeningAndSeatsIn(screening, seats));
        if (someSeatUnknown || someSeatAlreadyBooked) {
            throw new BookingValidationException("Der Sitzplatz ist nicht mehr verfügbar.");
        }

        String strippedCardNumber = payment.cardNumber().replace(" ", "");
        if (!strippedCardNumber.matches("\\d{13,19}")) {
            throw new InvalidCardNumberException("Die Kartennummer muss aus 13 bis 19 Ziffern bestehen.");
        }
        if (!passesLuhnCheck(strippedCardNumber)) {
            throw new InvalidCardNumberException("Die Kartennummer ist ungültig.");
        }

        Booking booking = new Booking(generateBookingNumber(), screening, new LinkedHashSet<>(seats),
                LocalDateTime.now());
        bookingRepository.save(booking);

        BigDecimal total = calculateTotal(screening.getPrice(), seats.size());
        List<BookingSeatDto> seatDtos = bookingMapper.toBookingSeatDtoList(seats);
        return bookingMapper.toBookingConfirmationDto(booking, seatDtos, screening.getPrice(), total);
    }

    @Transactional(readOnly = true)
    public BookingConfirmationDto getBookingConfirmation(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumberWithScreeningAndSeats(bookingNumber)
                .orElseThrow(BookingNotFoundException::new);

        Screening screening = booking.getScreening();
        List<BookingSeatDto> seatDtos = bookingMapper.toBookingSeatDtoList(new ArrayList<>(booking.getSeats()));
        BigDecimal total = calculateTotal(screening.getPrice(), booking.getSeats().size());
        return bookingMapper.toBookingConfirmationDto(booking, seatDtos, screening.getPrice(), total);
    }

    private BigDecimal calculateTotal(BigDecimal pricePerSeat, int seatCount) {
        return pricePerSeat.multiply(BigDecimal.valueOf(seatCount));
    }

    private boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private String generateBookingNumber() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
