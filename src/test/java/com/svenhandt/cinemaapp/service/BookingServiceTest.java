package com.svenhandt.cinemaapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Booking;
import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.entities.Weekday;
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

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void returnsBookingsOfScreening() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        Booking booking = new Booking("B-1", screening, Set.of(), LocalDateTime.now());
        when(bookingRepository.findByScreening(screening)).thenReturn(List.of(booking));

        List<Booking> result = bookingService.findByScreening(screening);

        assertThat(result).containsExactly(booking);
    }

    @Test
    void returnsEmptyListWhenScreeningHasNoBookings() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        when(bookingRepository.findByScreening(screening)).thenReturn(List.of());

        List<Booking> result = bookingService.findByScreening(screening);

        assertThat(result).isEmpty();
    }

    @Test
    void preparesBookingInitOnHappyPath() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        BigDecimal price = new BigDecimal("7.50");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), price);
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat1 = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat1, "id", 10L);
        Seat seat2 = new Seat(auditorium, 1, 2, 1);
        ReflectionTestUtils.setField(seat2, "id", 11L);
        when(seatRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(seat1, seat2));
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat1, seat2))).thenReturn(List.of());

        List<BookingSeatDto> seatDtos = List.of(new BookingSeatDto(10L, 1, 1), new BookingSeatDto(11L, 1, 2));
        when(bookingMapper.toBookingSeatDtoList(List.of(seat1, seat2))).thenReturn(seatDtos);

        BigDecimal expectedTotal = price.multiply(BigDecimal.valueOf(2));
        BookingInitDto expected = new BookingInitDto(1L, "Der Name der Rose", Weekday.MO, LocalTime.of(17, 0),
                "Saal 1", seatDtos, expectedTotal, 9, 2026);
        when(bookingMapper.toBookingInitDto(eq(screening), eq(seatDtos), eq(expectedTotal), anyInt(), anyInt()))
                .thenReturn(expected);

        BookingInitDto result = bookingService.initBooking(1L, List.of(10L, 11L));

        assertThat(result).isSameAs(expected);
    }

    @Test
    void throwsWhenNoSeatSelected() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.50"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        assertThatThrownBy(() -> bookingService.initBooking(1L, List.of()))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Bitte wählen Sie mindestens einen Sitzplatz aus.");

        verify(seatRepository, never()).findAllById(any());
        verify(bookingRepository, never()).findByScreeningAndSeatsIn(any(), any());
    }

    @Test
    void throwsWhenScreeningDoesNotExist() {
        when(screeningRepository.findByIdWithMovieAndAuditorium(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.initBooking(99L, List.of(10L)))
                .isInstanceOf(ScreeningNotFoundException.class);

        verify(seatRepository, never()).findAllById(any());
        verify(bookingRepository, never()).findByScreeningAndSeatsIn(any(), any());
    }

    @Test
    void throwsWhenSeatAlreadyBookedForScreening() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.50"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));

        Booking existingBooking = new Booking("B-1", screening, Set.of(seat), LocalDateTime.now());
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat)))
                .thenReturn(List.of(existingBooking));

        assertThatThrownBy(() -> bookingService.initBooking(1L, List.of(10L)))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Der Sitzplatz ist nicht mehr verfügbar.");

        verify(bookingMapper, never()).toBookingInitDto(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void completesBookingOnHappyPath() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        BigDecimal price = new BigDecimal("7.50");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), price);
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat1 = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat1, "id", 10L);
        Seat seat2 = new Seat(auditorium, 1, 2, 1);
        ReflectionTestUtils.setField(seat2, "id", 11L);
        when(seatRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(seat1, seat2));
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat1, seat2))).thenReturn(List.of());

        List<BookingSeatDto> seatDtos = List.of(new BookingSeatDto(10L, 1, 1), new BookingSeatDto(11L, 1, 2));
        when(bookingMapper.toBookingSeatDtoList(List.of(seat1, seat2))).thenReturn(seatDtos);

        BigDecimal expectedTotal = price.multiply(BigDecimal.valueOf(2));
        BookingConfirmationDto expected = new BookingConfirmationDto("BK-A1B2C3D4", "Der Name der Rose", Weekday.MO,
                LocalTime.of(17, 0), "Saal 1", seatDtos, price, expectedTotal, LocalDateTime.now());
        when(bookingMapper.toBookingConfirmationDto(any(Booking.class), eq(seatDtos), eq(price), eq(expectedTotal)))
                .thenReturn(expected);

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532015112830366", 9, 2026, "123");
        BookingConfirmationDto result = bookingService.completeBooking(1L, List.of(10L, 11L), payment);

        assertThat(result).isSameAs(expected);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertThat(savedBooking.getScreening()).isEqualTo(screening);
        assertThat(savedBooking.getSeats()).containsExactlyInAnyOrder(seat1, seat2);
        assertThat(savedBooking.getBookingNumber()).matches(Pattern.compile("BK-[A-F0-9]{8}"));
        assertThat(savedBooking.getBookingDate()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void throwsWhenScreeningDoesNotExistOnCompletion() {
        when(screeningRepository.findByIdWithMovieAndAuditorium(99L)).thenReturn(Optional.empty());

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532015112830366", 9, 2026, "123");
        assertThatThrownBy(() -> bookingService.completeBooking(99L, List.of(10L), payment))
                .isInstanceOf(ScreeningNotFoundException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void throwsWhenNoSeatSelectedOnCompletion() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.50"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532015112830366", 9, 2026, "123");
        assertThatThrownBy(() -> bookingService.completeBooking(1L, List.of(), payment))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Bitte wählen Sie mindestens einen Sitzplatz aus.");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void throwsWhenSeatNoLongerAvailableOnCompletion() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.50"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));

        Booking existingBooking = new Booking("B-1", screening, Set.of(seat), LocalDateTime.now());
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat)))
                .thenReturn(List.of(existingBooking));

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532015112830366", 9, 2026, "123");
        assertThatThrownBy(() -> bookingService.completeBooking(1L, List.of(10L), payment))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Der Sitzplatz ist nicht mehr verfügbar.");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void throwsWhenCardNumberHasInvalidLength() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.50"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat))).thenReturn(List.of());

        PaymentDto payment = new PaymentDto("Max Mustermann", "123456789012", 9, 2026, "123");
        assertThatThrownBy(() -> bookingService.completeBooking(1L, List.of(10L), payment))
                .isInstanceOf(InvalidCardNumberException.class)
                .hasMessage("Die Kartennummer muss aus 13 bis 19 Ziffern bestehen.");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void throwsWhenCardNumberFailsLuhnCheck() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.50"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat))).thenReturn(List.of());

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532015112830367", 9, 2026, "123");
        assertThatThrownBy(() -> bookingService.completeBooking(1L, List.of(10L), payment))
                .isInstanceOf(InvalidCardNumberException.class)
                .hasMessage("Die Kartennummer ist ungültig.");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void acceptsCardNumberWithSpacesAfterStripping() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        BigDecimal price = new BigDecimal("7.50");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), price);
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat))).thenReturn(List.of());

        List<BookingSeatDto> seatDtos = List.of(new BookingSeatDto(10L, 1, 1));
        when(bookingMapper.toBookingSeatDtoList(List.of(seat))).thenReturn(seatDtos);
        when(bookingMapper.toBookingConfirmationDto(any(Booking.class), eq(seatDtos), eq(price), eq(price)))
                .thenReturn(new BookingConfirmationDto("BK-A1B2C3D4", "Der Name der Rose", Weekday.MO,
                        LocalTime.of(17, 0), "Saal 1", seatDtos, price, price, LocalDateTime.now()));

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532 0151 1283 0366", 9, 2026, "123");
        bookingService.completeBooking(1L, List.of(10L), payment);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void generatesUniqueBookingNumberPerCall() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        BigDecimal price = new BigDecimal("7.50");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), price);
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        when(seatRepository.findAllById(List.of(10L))).thenReturn(List.of(seat));
        when(bookingRepository.findByScreeningAndSeatsIn(screening, List.of(seat))).thenReturn(List.of());
        when(bookingMapper.toBookingSeatDtoList(List.of(seat))).thenReturn(List.of(new BookingSeatDto(10L, 1, 1)));

        PaymentDto payment = new PaymentDto("Max Mustermann", "4532015112830366", 9, 2026, "123");
        bookingService.completeBooking(1L, List.of(10L), payment);
        bookingService.completeBooking(1L, List.of(10L), payment);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(2)).save(bookingCaptor.capture());
        List<Booking> savedBookings = bookingCaptor.getAllValues();
        assertThat(savedBookings.get(0).getBookingNumber()).isNotEqualTo(savedBookings.get(1).getBookingNumber());
    }

    @Test
    void returnsConfirmationForExistingBookingNumber() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        BigDecimal price = new BigDecimal("7.50");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0), price);
        Seat seat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(seat, "id", 10L);
        Booking booking = new Booking("BK-A1B2C3D4", screening, Set.of(seat), LocalDateTime.now());
        when(bookingRepository.findByBookingNumberWithScreeningAndSeats("BK-A1B2C3D4"))
                .thenReturn(Optional.of(booking));

        List<BookingSeatDto> seatDtos = List.of(new BookingSeatDto(10L, 1, 1));
        when(bookingMapper.toBookingSeatDtoList(any())).thenReturn(seatDtos);

        BookingConfirmationDto expected = new BookingConfirmationDto("BK-A1B2C3D4", "Der Name der Rose", Weekday.MO,
                LocalTime.of(17, 0), "Saal 1", seatDtos, price, price, booking.getBookingDate());
        when(bookingMapper.toBookingConfirmationDto(eq(booking), eq(seatDtos), eq(price), eq(price)))
                .thenReturn(expected);

        BookingConfirmationDto result = bookingService.getBookingConfirmation("BK-A1B2C3D4");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void throwsWhenBookingNumberDoesNotExist() {
        when(bookingRepository.findByBookingNumberWithScreeningAndSeats("BK-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingConfirmation("BK-UNKNOWN"))
                .isInstanceOf(BookingNotFoundException.class);
    }
}
