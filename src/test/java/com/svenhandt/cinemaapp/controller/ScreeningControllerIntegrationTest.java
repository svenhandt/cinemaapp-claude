package com.svenhandt.cinemaapp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class ScreeningControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void screeningsOverviewPageRendersImportedMovies() {
        assertThat(mvc.get().uri("/"))
                .hasStatusOk()
                .hasViewName("screenings-overview")
                .bodyText().contains("Der Name der Rose");
    }
}
