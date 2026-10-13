package com.svenhandt.cinemaapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.svenhandt.cinemaapp.domain.entities.Weekday;
import com.svenhandt.cinemaapp.dto.BookingConfirmationDto;
import com.svenhandt.cinemaapp.dto.BookingInitDto;
import com.svenhandt.cinemaapp.dto.BookingSeatDto;
import com.svenhandt.cinemaapp.dto.PaymentDto;
import com.svenhandt.cinemaapp.exception.BookingNotFoundException;
import com.svenhandt.cinemaapp.exception.BookingValidationException;
import com.svenhandt.cinemaapp.exception.InvalidCardNumberException;
import com.svenhandt.cinemaapp.exception.ScreeningNotFoundException;
import com.svenhandt.cinemaapp.service.BookingPdfService;
import com.svenhandt.cinemaapp.service.BookingService;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingPdfService bookingPdfService;

    @Test
    void rendersBookingFormOnSuccess() {
        BookingInitDto booking = new BookingInitDto(1L, "Der Name der Rose", Weekday.MO, LocalTime.of(17, 0),
                "Saal 1", List.of(new BookingSeatDto(10L, 1, 1)), new BigDecimal("7.50"), 9, 2026);
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(booking);

        assertThat(mvc.post().uri("/bookings/init").param("screeningId", "1").param("seatIds", "10"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .model().containsEntry("booking", booking);
    }

    @Test
    void redirectsWithErrorWhenNoSeatSelected() {
        when(bookingService.initBooking(1L, List.of()))
                .thenThrow(new BookingValidationException("Bitte wählen Sie mindestens einen Sitzplatz aus."));

        assertThat(mvc.post().uri("/bookings/init").param("screeningId", "1"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/screenings/1")
                .flash().containsEntry("error", "Bitte wählen Sie mindestens einen Sitzplatz aus.");
    }

    @Test
    void redirectsWithErrorWhenSeatNoLongerAvailable() {
        when(bookingService.initBooking(1L, List.of(10L)))
                .thenThrow(new BookingValidationException("Der Sitzplatz ist nicht mehr verfügbar."));

        assertThat(mvc.post().uri("/bookings/init").param("screeningId", "1").param("seatIds", "10"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/screenings/1")
                .flash().containsEntry("error", "Der Sitzplatz ist nicht mehr verfügbar.");
    }

    @Test
    void returnsNotFoundWhenScreeningDoesNotExist() {
        when(bookingService.initBooking(99L, List.of(10L))).thenThrow(new ScreeningNotFoundException());

        assertThat(mvc.post().uri("/bookings/init").param("screeningId", "99").param("seatIds", "10"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    private BookingInitDto bookingInitDtoFixture() {
        return new BookingInitDto(1L, "Der Name der Rose", Weekday.MO, LocalTime.of(17, 0), "Saal 1",
                List.of(new BookingSeatDto(10L, 1, 1)), new BigDecimal("7.50"), 9, 2026);
    }

    @Test
    void completesBookingAndShowsConfirmationOnSuccess() {
        BookingConfirmationDto confirmation = new BookingConfirmationDto("BK-A1B2C3D4", "Der Name der Rose",
                Weekday.MO, LocalTime.of(17, 0), "Saal 1", List.of(new BookingSeatDto(10L, 1, 1)),
                new BigDecimal("7.50"), new BigDecimal("7.50"), LocalDateTime.now());
        when(bookingService.completeBooking(eq(1L), eq(List.of(10L)), any(PaymentDto.class)))
                .thenReturn(confirmation);

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatusOk()
                .hasViewName("booking-confirmation")
                .model().containsEntry("confirmation", confirmation);
    }

    @Test
    void redisplaysFormWhenCardholderNameIsBlank() {
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Bitte geben Sie den Karteninhaber ein.");
    }

    @Test
    void redisplaysFormWhenCardNumberIsBlank() {
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Bitte geben Sie die Kartennummer ein.");
    }

    @Test
    void redisplaysFormWhenExpiryMonthIsOutOfRange() {
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "13")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Bitte geben Sie das Gültigkeitsdatum im Format MM/YY ein.");
    }

    @Test
    void redisplaysFormWhenSecurityCodeIsBlank() {
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", ""))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Bitte geben Sie die Prüfziffer ein.");
    }

    @Test
    void redisplaysFormWhenSecurityCodeHasWrongDigitCount() {
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "12"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Die Prüfziffer muss aus 3 oder 4 Ziffern bestehen.");
    }

    @Test
    void redisplaysFormWhenCardNumberHasInvalidLength() {
        when(bookingService.completeBooking(eq(1L), eq(List.of(10L)), any(PaymentDto.class)))
                .thenThrow(new InvalidCardNumberException("Die Kartennummer muss aus 13 bis 19 Ziffern bestehen."));
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "123456789012")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Die Kartennummer muss aus 13 bis 19 Ziffern bestehen.");
    }

    @Test
    void redisplaysFormWhenCardNumberFailsLuhnCheck() {
        when(bookingService.completeBooking(eq(1L), eq(List.of(10L)), any(PaymentDto.class)))
                .thenThrow(new InvalidCardNumberException("Die Kartennummer ist ungültig."));
        when(bookingService.initBooking(1L, List.of(10L))).thenReturn(bookingInitDtoFixture());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830367")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains("Die Kartennummer ist ungültig.");
    }

    @Test
    void redirectsWhenSeatNoLongerAvailableOnCompletion() {
        when(bookingService.completeBooking(eq(1L), eq(List.of(10L)), any(PaymentDto.class)))
                .thenThrow(new BookingValidationException("Der Sitzplatz ist nicht mehr verfügbar."));

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatus3xxRedirection()
                .hasRedirectedUrl("/screenings/1")
                .flash().containsEntry("error", "Der Sitzplatz ist nicht mehr verfügbar.");
    }

    @Test
    void returnsNotFoundWhenScreeningDoesNotExistOnCompletion() {
        when(bookingService.completeBooking(eq(1L), eq(List.of(10L)), any(PaymentDto.class)))
                .thenThrow(new ScreeningNotFoundException());

        assertThat(mvc.post().uri("/bookings")
                .param("screeningId", "1")
                .param("seatIds", "10")
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void returnsPdfForExistingBooking() {
        BookingConfirmationDto confirmation = new BookingConfirmationDto("BK-A1B2C3D4", "Der Name der Rose",
                Weekday.MO, LocalTime.of(17, 0), "Saal 1", List.of(new BookingSeatDto(10L, 1, 1)),
                new BigDecimal("7.50"), new BigDecimal("7.50"), LocalDateTime.now());
        byte[] pdfBytes = "%PDF-fake".getBytes();
        when(bookingService.getBookingConfirmation("BK-A1B2C3D4")).thenReturn(confirmation);
        when(bookingPdfService.generateConfirmationPdf(confirmation)).thenReturn(pdfBytes);

        assertThat(mvc.get().uri("/bookings/BK-A1B2C3D4/pdf"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_PDF)
                .body().isEqualTo(pdfBytes);
    }

    @Test
    void returnsNotFoundForUnknownBookingNumber() {
        when(bookingService.getBookingConfirmation("BK-UNKNOWN")).thenThrow(new BookingNotFoundException());

        assertThat(mvc.get().uri("/bookings/BK-UNKNOWN/pdf")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
