package com.svenhandt.cinemaapp.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Movie;
import com.svenhandt.cinemaapp.domain.entities.Screening;
import com.svenhandt.cinemaapp.domain.entities.Seat;
import com.svenhandt.cinemaapp.domain.entities.Weekday;
import com.svenhandt.cinemaapp.domain.repositories.AuditoriumRepository;
import com.svenhandt.cinemaapp.domain.repositories.MovieRepository;
import com.svenhandt.cinemaapp.domain.repositories.ScreeningRepository;
import com.svenhandt.cinemaapp.dto.AuditoriumDto;
import com.svenhandt.cinemaapp.dto.MovieScreeningsDto;
import com.svenhandt.cinemaapp.dto.ScreeningDetailsDto;
import com.svenhandt.cinemaapp.dto.WeekdayScreeningsDto;
import com.svenhandt.cinemaapp.exception.ScreeningImportException;
import com.svenhandt.cinemaapp.exception.ScreeningNotFoundException;
import com.svenhandt.cinemaapp.mapper.ScreeningMapper;

@Service
public class ScreeningService {

    private static final Pattern TITLE_LINE_PATTERN = Pattern.compile("^(\\S[^;]*);$");
    private static final Pattern SCREENINGS_LINE_PATTERN = Pattern.compile("^ {4}(\\S.*)$");
    private static final Pattern SCREENING_ENTRY_PATTERN =
            Pattern.compile("^(Mo|Di|Mi|Do|Fr|Sa|So)/(\\d{2}:\\d{2})/([^/]+)/(\\d+(?:\\.\\d+)?)$");

    private final MovieRepository movieRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final ScreeningRepository screeningRepository;
    private final ScreeningMapper screeningMapper;
    private final AuditoriumService auditoriumService;
    private final BookingService bookingService;

    public ScreeningService(MovieRepository movieRepository, AuditoriumRepository auditoriumRepository,
            ScreeningRepository screeningRepository, ScreeningMapper screeningMapper,
            AuditoriumService auditoriumService, BookingService bookingService) {
        this.movieRepository = movieRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.screeningRepository = screeningRepository;
        this.screeningMapper = screeningMapper;
        this.auditoriumService = auditoriumService;
        this.bookingService = bookingService;
    }

    @Transactional(readOnly = true)
    public ScreeningDetailsDto getScreeningDetails(Long screeningId) {
        Screening screening = screeningRepository.findByIdWithMovieAndAuditorium(screeningId)
                .orElseThrow(ScreeningNotFoundException::new);
        AuditoriumDto auditoriumDto =
                auditoriumService.getAuditoriumDetails(screening.getAuditorium(), bookedSeatIds(screening));
        return screeningMapper.toScreeningDetailsDto(screening, auditoriumDto);
    }

    private Set<Long> bookedSeatIds(Screening screening) {
        return bookingService.findByScreening(screening).stream()
                .flatMap(booking -> booking.getSeats().stream())
                .map(Seat::getId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<MovieScreeningsDto> findScreeningsOverview() {
        List<Screening> screenings = screeningRepository.findAllWithMovieAndAuditorium();

        Map<Movie, List<Screening>> screeningsByMovie = screenings.stream()
                .collect(Collectors.groupingBy(Screening::getMovie, LinkedHashMap::new, Collectors.toList()));

        return screeningsByMovie.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getTitle()))
                .map(entry -> screeningMapper.toMovieScreeningsDto(entry.getKey(), toWeekdaySchedule(entry.getValue())))
                .toList();
    }

    private List<WeekdayScreeningsDto> toWeekdaySchedule(List<Screening> screenings) {
        Map<Weekday, List<Screening>> screeningsByWeekday = screenings.stream()
                .collect(Collectors.groupingBy(Screening::getWeekday, () -> new EnumMap<>(Weekday.class),
                        Collectors.toList()));

        Comparator<Screening> byStartTime = Comparator.comparing(Screening::getStartTime);

        return screeningsByWeekday.entrySet().stream()
                .map(entry -> new WeekdayScreeningsDto(entry.getKey(),
                        screeningMapper.toScreeningDtoList(entry.getValue().stream().sorted(byStartTime).toList())))
                .toList();
    }

    @Transactional
    public List<Movie> importScreenings(String content) {
        List<ParsedMovie> parsedMovies = parseMovies(content);

        List<Movie> movies = new ArrayList<>();
        for (ParsedMovie parsedMovie : parsedMovies) {
            Movie movie = movieRepository.findByTitle(parsedMovie.title())
                    .orElseGet(() -> importMovie(parsedMovie));
            movies.add(movie);
        }

        return movies;
    }

    private Movie importMovie(ParsedMovie parsedMovie) {
        Movie movie = movieRepository.save(new Movie(parsedMovie.title()));

        List<Screening> screenings = new ArrayList<>();
        for (ParsedScreening parsedScreening : parsedMovie.screenings()) {
            Auditorium auditorium = auditoriumRepository.findByName(parsedScreening.auditoriumName())
                    .orElseThrow(() -> new ScreeningImportException(
                            "Auditorium '" + parsedScreening.auditoriumName() + "' referenced by movie '"
                                    + parsedMovie.title() + "' does not exist."));
            screenings.add(new Screening(movie, auditorium, parsedScreening.weekday(),
                    parsedScreening.startTime(), parsedScreening.price()));
        }
        screeningRepository.saveAll(screenings);

        return movie;
    }

    private List<ParsedMovie> parseMovies(String content) {
        if (StringUtils.isBlank(content)) {
            throw new ScreeningImportException("The screenings file is empty.");
        }

        List<ParsedMovie> movies = new ArrayList<>();
        String currentTitle = null;
        List<ParsedScreening> currentScreenings = null;

        for (String line : content.lines().toList()) {
            if (isIndented(line)) {
                if (currentTitle == null) {
                    throw new ScreeningImportException(
                            "Screenings are not preceded by a movie title: '" + line + "'.");
                }
                String strippedLine = stripIndentation(currentTitle, line);
                currentScreenings.addAll(parseScreeningsLine(currentTitle, strippedLine));
            } else {
                if (currentTitle != null) {
                    movies.add(toParsedMovie(currentTitle, currentScreenings));
                }
                currentTitle = parseTitle(line);
                currentScreenings = new ArrayList<>();
            }
        }
        if (currentTitle != null) {
            movies.add(toParsedMovie(currentTitle, currentScreenings));
        }

        return movies;
    }

    private ParsedMovie toParsedMovie(String title, List<ParsedScreening> screenings) {
        if (CollectionUtils.isEmpty(screenings)) {
            throw new ScreeningImportException("The movie '" + title + "' has no screenings.");
        }
        return new ParsedMovie(title, screenings);
    }

    private boolean isIndented(String line) {
        return !line.isEmpty() && Character.isWhitespace(line.charAt(0));
    }

    private String parseTitle(String line) {
        Matcher matcher = TITLE_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new ScreeningImportException(
                    "Expected a movie title without leading whitespace, ending with ';': '" + line + "'.");
        }
        return matcher.group(1);
    }

    private String stripIndentation(String movieTitle, String line) {
        Matcher matcher = SCREENINGS_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new ScreeningImportException(
                    "Screenings for movie '" + movieTitle + "' are not indented by exactly 4 spaces: '" + line
                            + "'.");
        }
        return matcher.group(1);
    }

    private List<ParsedScreening> parseScreeningsLine(String movieTitle, String strippedLine) {
        List<ParsedScreening> screenings = new ArrayList<>();
        for (String entry : StringUtils.split(strippedLine, ',')) {
            screenings.add(parseScreeningEntry(movieTitle, entry));
        }
        return screenings;
    }

    private ParsedScreening parseScreeningEntry(String movieTitle, String entry) {
        Matcher matcher = SCREENING_ENTRY_PATTERN.matcher(entry);
        if (!matcher.matches()) {
            throw new ScreeningImportException(
                    "Screening entry for movie '" + movieTitle
                            + "' does not match the format Weekday/Time/Auditorium/Price: '" + entry + "'.");
        }

        Weekday weekday = Weekday.valueOf(matcher.group(1).toUpperCase());

        LocalTime startTime;
        try {
            startTime = LocalTime.parse(matcher.group(2));
        } catch (DateTimeParseException e) {
            throw new ScreeningImportException(
                    "Screening entry for movie '" + movieTitle + "' has an invalid time: '" + entry + "'.");
        }

        String auditoriumName = matcher.group(3);
        BigDecimal price = new BigDecimal(matcher.group(4));
        if (price.signum() <= 0) {
            throw new ScreeningImportException(
                    "Screening entry for movie '" + movieTitle + "' has a non-positive price: '" + entry + "'.");
        }

        return new ParsedScreening(weekday, startTime, auditoriumName, price);
    }

    private record ParsedScreening(Weekday weekday, LocalTime startTime, String auditoriumName, BigDecimal price) {
    }

    private record ParsedMovie(String title, List<ParsedScreening> screenings) {
    }
}
