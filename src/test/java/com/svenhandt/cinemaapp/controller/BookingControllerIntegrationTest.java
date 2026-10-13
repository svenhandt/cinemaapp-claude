package com.svenhandt.cinemaapp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.repositories.ScreeningRepository;
import com.svenhandt.cinemaapp.domain.repositories.SeatRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    void rendersBookingFormForSelectedScreeningAndSeat() {
        List<Screening> screenings = screeningRepository.findAllWithMovieAndAuditorium();
        Screening screening = screenings.get(0);
        Seat seat = seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(screening.getAuditorium())
                .get(0);

        assertThat(mvc.post().uri("/bookings/init")
                .param("screeningId", String.valueOf(screening.getId()))
                .param("seatIds", String.valueOf(seat.getId())))
                .hasStatusOk()
                .hasViewName("booking-form")
                .bodyText().contains(screening.getMovie().getTitle(), "Jetzt buchen");
    }

    @Test
    void returnsPdfForCompletedBooking() throws Exception {
        List<Screening> screenings = screeningRepository.findAllWithMovieAndAuditorium();
        Screening screening = screenings.get(0);
        Seat seat = seatRepository.findByAuditoriumOrderByRowNumberAscColumnPositionAsc(screening.getAuditorium())
                .get(0);

        MvcTestResult bookingResult = mvc.post().uri("/bookings")
                .param("screeningId", String.valueOf(screening.getId()))
                .param("seatIds", String.valueOf(seat.getId()))
                .param("cardholderName", "Max Mustermann")
                .param("cardNumber", "4532015112830366")
                .param("expiryMonth", "9")
                .param("expiryYear", "2026")
                .param("securityCode", "123")
                .exchange();

        assertThat(bookingResult).hasStatusOk().hasViewName("booking-confirmation");

        String bookingPage = bookingResult.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("Buchungsnummer: <strong>(BK-[A-F0-9]{8})</strong>").matcher(bookingPage);
        assertThat(matcher.find()).isTrue();
        String bookingNumber = matcher.group(1);

        assertThat(mvc.get().uri("/bookings/{bookingNumber}/pdf", bookingNumber))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_PDF)
                .body().isNotEmpty();
    }
}
