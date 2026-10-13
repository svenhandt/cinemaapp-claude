package com.svenhandt.cinemaapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import com.svenhandt.cinemaapp.domain.entities.Weekday;
import com.svenhandt.cinemaapp.dto.BookingConfirmationDto;
import com.svenhandt.cinemaapp.dto.BookingSeatDto;

class BookingPdfServiceTest {

    private final BookingPdfService bookingPdfService = new BookingPdfService();

    @Test
    void generatesNonEmptyPdfStartingWithPdfHeader() {
        byte[] pdf = bookingPdfService.generateConfirmationPdf(confirmationFixture());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void includesAllRelevantBookingDataInPdfText() throws Exception {
        byte[] pdf = bookingPdfService.generateConfirmationPdf(confirmationFixture());

        String text = extractText(pdf);

        assertThat(text).contains("BK-A1B2C3D4");
        assertThat(text).contains("Reihe 1, Platz 1");
        assertThat(text).contains("Reihe 1, Platz 2");
        assertThat(text).contains("15.00");
    }

    private String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            return extractor.getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    private BookingConfirmationDto confirmationFixture() {
        List<BookingSeatDto> seats = List.of(new BookingSeatDto(10L, 1, 1), new BookingSeatDto(11L, 1, 2));
        return new BookingConfirmationDto("BK-A1B2C3D4", "Der Name der Rose", Weekday.MO, LocalTime.of(17, 0),
                "Saal 1", seats, new BigDecimal("7.50"), new BigDecimal("15.00"),
                LocalDateTime.of(2026, 9, 25, 14, 0));
    }
}
