package com.svenhandt.cinemaapp.init;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.svenhandt.cinemaapp.service.AuditoriumService;
import com.svenhandt.cinemaapp.service.ScreeningService;

@Component
@Profile("!test")
public class CinemaAppInit implements ApplicationRunner {

    private static final String AUDITORIUM_FILES_LOCATION = "classpath:cinemaapp/*.txt";
    private static final String SCREENINGS_FILE_NAME = "Vorstellungen.txt";
    private static final String SCREENINGS_FILE_LOCATION = "classpath:cinemaapp/" + SCREENINGS_FILE_NAME;
    private static final String TXT_FILE_EXTENSION = ".txt";

    private final ResourcePatternResolver resourcePatternResolver;
    private final AuditoriumService auditoriumService;
    private final ScreeningService screeningService;

    public CinemaAppInit(ResourcePatternResolver resourcePatternResolver, AuditoriumService auditoriumService,
            ScreeningService screeningService) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.auditoriumService = auditoriumService;
        this.screeningService = screeningService;
    }

    @Override
    public void run(ApplicationArguments args) {
        initCinemaApp();
    }

    private void initCinemaApp() {
        initAuditoriums();
        initScreenings();
    }

    private void initAuditoriums() {
        for (Resource resource : findAuditoriumResources()) {
            String auditoriumName = auditoriumName(resource);
            String seatingArrangement = readContent(resource);
            auditoriumService.importAuditorium(auditoriumName, seatingArrangement);
        }
    }

    private void initScreenings() {
        Resource resource = resourcePatternResolver.getResource(SCREENINGS_FILE_LOCATION);
        String content = readContent(resource);
        screeningService.importScreenings(content);
    }

    private Resource[] findAuditoriumResources() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(AUDITORIUM_FILES_LOCATION);
            return Arrays.stream(resources)
                    .filter(resource -> !SCREENINGS_FILE_NAME.equals(resource.getFilename()))
                    .toArray(Resource[]::new);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list auditorium files at " + AUDITORIUM_FILES_LOCATION, e);
        }
    }

    private String auditoriumName(Resource resource) {
        return StringUtils.removeEnd(resource.getFilename(), TXT_FILE_EXTENSION);
    }

    private String readContent(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read auditorium file " + resource.getFilename(), e);
        }
    }
}
