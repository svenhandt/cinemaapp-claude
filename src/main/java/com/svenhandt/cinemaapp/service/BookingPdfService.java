package com.svenhandt.cinemaapp.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import com.svenhandt.cinemaapp.dto.BookingConfirmationDto;
import com.svenhandt.cinemaapp.dto.BookingSeatDto;

@Service
public class BookingPdfService {

    private static final DateTimeFormatter BOOKING_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateConfirmationPdf(BookingConfirmationDto confirmation) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            document.add(new Paragraph("Buchungsbestätigung", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Buchungsnummer: " + confirmation.bookingNumber(), boldFont));
            document.add(new Paragraph(confirmation.movieTitle(), boldFont));
            document.add(new Paragraph(
                    StringUtils.capitalize(confirmation.weekday().name().toLowerCase(Locale.ROOT)) + ", "
                            + confirmation.startTime() + " Uhr · " + confirmation.auditoriumName(),
                    normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Sitzplätze:", boldFont));
            for (BookingSeatDto seat : confirmation.seats()) {
                document.add(new Paragraph("Reihe " + seat.rowNumber() + ", Platz " + seat.seatNumber(), normalFont));
            }
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Preis pro Platz: " + formatAmount(confirmation.pricePerSeat()), normalFont));
            document.add(new Paragraph("Gesamtsumme: " + formatAmount(confirmation.total()), boldFont));
            document.add(new Paragraph("Gebucht am: " + confirmation.bookingDate().format(BOOKING_DATE_FORMAT),
                    normalFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("PDF konnte nicht erstellt werden.", e);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.ROOT, "%.2f €", amount);
    }
}
