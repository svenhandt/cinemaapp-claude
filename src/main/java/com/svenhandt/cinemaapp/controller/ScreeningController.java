package com.svenhandt.cinemaapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.svenhandt.cinemaapp.service.ScreeningService;

@Controller
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @GetMapping("/")
    public String getScreeningsOverview(Model model) {
        model.addAttribute("movieScreenings", screeningService.findScreeningsOverview());
        return "screenings-overview";
    }

    @GetMapping("/screenings/{screeningId}")
    public String getScreeningDetails(@PathVariable Long screeningId, Model model) {
        model.addAttribute("screening", screeningService.getScreeningDetails(screeningId));
        return "screening-details";
    }
}
