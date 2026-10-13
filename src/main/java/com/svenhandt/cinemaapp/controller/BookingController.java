package com.svenhandt.cinemaapp.controller;

import java.util.List;

import org.apache.commons.collections4.ListUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.svenhandt.cinemaapp.dto.BookingConfirmationDto;
import com.svenhandt.cinemaapp.dto.BookingInitDto;
import com.svenhandt.cinemaapp.dto.PaymentDto;
import com.svenhandt.cinemaapp.exception.BookingValidationException;
import com.svenhandt.cinemaapp.exception.InvalidCardNumberException;
import com.svenhandt.cinemaapp.service.BookingPdfService;
import com.svenhandt.cinemaapp.service.BookingService;

import jakarta.validation.Valid;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final BookingPdfService bookingPdfService;

    public BookingController(BookingService bookingService, BookingPdfService bookingPdfService) {
        this.bookingService = bookingService;
        this.bookingPdfService = bookingPdfService;
    }

    @PostMapping("/bookings/init")
    public String initBooking(@RequestParam Long screeningId, @RequestParam(required = false) List<Long> seatIds,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            BookingInitDto booking = bookingService.initBooking(screeningId, ListUtils.emptyIfNull(seatIds));
            model.addAttribute("booking", booking);
            model.addAttribute("payment",
                    new PaymentDto(null, null, booking.currentMonth(), booking.currentYear(), null));
            return "booking-form";
        } catch (BookingValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/screenings/" + screeningId;
        }
    }

    @PostMapping("/bookings")
    public String completeBooking(@RequestParam Long screeningId, @RequestParam(required = false) List<Long> seatIds,
            @Valid @ModelAttribute("payment") PaymentDto payment, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        List<Long> seats = ListUtils.emptyIfNull(seatIds);

        if (bindingResult.hasErrors()) {
            return redisplayBookingForm(screeningId, seats, model);
        }

        try {
            BookingConfirmationDto confirmation = bookingService.completeBooking(screeningId, seats, payment);
            model.addAttribute("confirmation", confirmation);
            return "booking-confirmation";
        } catch (InvalidCardNumberException e) {
            model.addAttribute("cardNumberError", e.getMessage());
            return redisplayBookingForm(screeningId, seats, model);
        } catch (BookingValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/screenings/" + screeningId;
        }
    }

    @GetMapping("/bookings/{bookingNumber}/pdf")
    public ResponseEntity<byte[]> getBookingConfirmationPdf(@PathVariable String bookingNumber) {
        BookingConfirmationDto confirmation = bookingService.getBookingConfirmation(bookingNumber);
        byte[] pdf = bookingPdfService.generateConfirmationPdf(confirmation);

        ContentDisposition contentDisposition = ContentDisposition.inline().filename(bookingNumber + ".pdf").build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(pdf);
    }

    private String redisplayBookingForm(Long screeningId, List<Long> seatIds, Model model) {
        model.addAttribute("booking", bookingService.initBooking(screeningId, seatIds));
        return "booking-form";
    }
}
