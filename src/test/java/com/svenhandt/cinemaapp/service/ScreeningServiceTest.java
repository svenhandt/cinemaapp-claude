package com.svenhandt.cinemaapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.svenhandt.cinemaapp.domain.entities.Auditorium;
import com.svenhandt.cinemaapp.domain.entities.Booking;
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
import com.svenhandt.cinemaapp.dto.ScreeningDto;
import com.svenhandt.cinemaapp.dto.WeekdayScreeningsDto;
import com.svenhandt.cinemaapp.exception.ScreeningImportException;
import com.svenhandt.cinemaapp.exception.ScreeningNotFoundException;
import com.svenhandt.cinemaapp.mapper.ScreeningMapper;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @Mock
    private ScreeningRepository screeningRepository;

    @Mock
    private ScreeningMapper screeningMapper;

    @Mock
    private AuditoriumService auditoriumService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ScreeningService screeningService;

    @Test
    void importsMovieWithItsScreenings() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriumRepository.findByName("Saal 1")).thenReturn(Optional.of(new Auditorium("Saal 1")));
        when(auditoriumRepository.findByName("Saal 2")).thenReturn(Optional.of(new Auditorium("Saal 2")));

        String content = "Der Name der Rose;\n    Mo/17:00/Saal 1/7.00,Di/20:00/Saal 2/8.50";

        List<Movie> movies = screeningService.importScreenings(content);

        assertThat(movies).hasSize(1);
        Movie movie = movies.get(0);
        assertThat(movie.getTitle()).isEqualTo("Der Name der Rose");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Screening>> screeningsCaptor = ArgumentCaptor.forClass(List.class);
        verify(screeningRepository).saveAll(screeningsCaptor.capture());
        List<Screening> screenings = screeningsCaptor.getValue();

        assertThat(screenings).hasSize(2);

        Screening first = screenings.get(0);
        assertThat(first.getMovie()).isSameAs(movie);
        assertThat(first.getWeekday()).isEqualTo(Weekday.MO);
        assertThat(first.getStartTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(first.getAuditorium().getName()).isEqualTo("Saal 1");
        assertThat(first.getPrice()).isEqualByComparingTo(new BigDecimal("7.00"));

        Screening second = screenings.get(1);
        assertThat(second.getMovie()).isSameAs(movie);
        assertThat(second.getWeekday()).isEqualTo(Weekday.DI);
        assertThat(second.getStartTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(second.getAuditorium().getName()).isEqualTo("Saal 2");
        assertThat(second.getPrice()).isEqualByComparingTo(new BigDecimal("8.50"));
    }

    @Test
    void rejectsTitleLineWithoutSemicolon() {
        String content = "Der Name der Rose\n    Mo/17:00/Saal 1/7.00";

        assertThatThrownBy(() -> screeningService.importScreenings(content))
                .isInstanceOf(ScreeningImportException.class);

        verify(movieRepository, never()).save(any());
        verify(auditoriumRepository, never()).findByName(any());
        verify(screeningRepository, never()).saveAll(any());
    }

    @Test
    void rejectsScreeningsNotSeparatedByComma() {
        String content = "Der Name der Rose;\n    Mo/17:00/Saal 1/7.00 Di/20:00/Saal 2/8.50";

        assertThatThrownBy(() -> screeningService.importScreenings(content))
                .isInstanceOf(ScreeningImportException.class);

        verify(movieRepository, never()).save(any());
        verify(auditoriumRepository, never()).findByName(any());
        verify(screeningRepository, never()).saveAll(any());
    }

    @Test
    void rejectsScreeningsNotIndentedByFourSpaces() {
        String content = "Der Name der Rose;\n  Mo/17:00/Saal 1/7.00";

        assertThatThrownBy(() -> screeningService.importScreenings(content))
                .isInstanceOf(ScreeningImportException.class);

        verify(movieRepository, never()).save(any());
        verify(auditoriumRepository, never()).findByName(any());
        verify(screeningRepository, never()).saveAll(any());
    }

    @Test
    void rejectsScreeningReferencingUnknownAuditorium() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String content = "Der Name der Rose;\n    Mo/17:00/Saal 1/7.00";

        assertThatThrownBy(() -> screeningService.importScreenings(content))
                .isInstanceOf(ScreeningImportException.class);

        verify(auditoriumRepository).findByName("Saal 1");
        verify(screeningRepository, never()).saveAll(any());
    }

    @Test
    void skipsImportForMovieThatAlreadyExists() {
        Movie existingMovie = new Movie("Der Name der Rose");
        when(movieRepository.findByTitle("Der Name der Rose")).thenReturn(Optional.of(existingMovie));

        String content = "Der Name der Rose;\n    Mo/17:00/Saal 1/7.00";

        List<Movie> movies = screeningService.importScreenings(content);

        assertThat(movies).containsExactly(existingMovie);
        verify(movieRepository, never()).save(any());
        verify(auditoriumRepository, never()).findByName(any());
        verify(screeningRepository, never()).saveAll(any());
    }

    @Test
    void importsOnlyTheNewMovieWhenAnotherAlreadyExists() {
        Movie existingMovie = new Movie("Bereits vorhanden");
        when(movieRepository.findByTitle("Bereits vorhanden")).thenReturn(Optional.of(existingMovie));
        when(movieRepository.findByTitle("Neuer Film")).thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriumRepository.findByName("Saal 1")).thenReturn(Optional.of(new Auditorium("Saal 1")));

        String content = "Bereits vorhanden;\n    Mo/17:00/Saal 1/7.00\n"
                + "Neuer Film;\n    Di/20:00/Saal 1/8.50";

        List<Movie> movies = screeningService.importScreenings(content);

        assertThat(movies).extracting(Movie::getTitle).containsExactly("Bereits vorhanden", "Neuer Film");
        assertThat(movies.get(0)).isSameAs(existingMovie);

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        assertThat(movieCaptor.getValue().getTitle()).isEqualTo("Neuer Film");
    }

    @Test
    void returnsScreeningsGroupedByMovieOrderedByWeekdayAndStartTime() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening tuesdayEvening = new Screening(movie, auditorium, Weekday.DI, LocalTime.of(20, 0),
                new BigDecimal("8.50"));
        Screening mondayEvening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(20, 0),
                new BigDecimal("7.00"));
        Screening mondayAfternoon = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        when(screeningRepository.findAllWithMovieAndAuditorium())
                .thenReturn(List.of(tuesdayEvening, mondayEvening, mondayAfternoon));

        ScreeningDto mondayAfternoonDto = new ScreeningDto(1L, Weekday.MO, LocalTime.of(17, 0), "Saal 1",
                new BigDecimal("7.00"));
        ScreeningDto mondayEveningDto = new ScreeningDto(2L, Weekday.MO, LocalTime.of(20, 0), "Saal 1",
                new BigDecimal("7.00"));
        ScreeningDto tuesdayEveningDto = new ScreeningDto(3L, Weekday.DI, LocalTime.of(20, 0), "Saal 1",
                new BigDecimal("8.50"));
        when(screeningMapper.toScreeningDtoList(List.of(mondayAfternoon, mondayEvening)))
                .thenReturn(List.of(mondayAfternoonDto, mondayEveningDto));
        when(screeningMapper.toScreeningDtoList(List.of(tuesdayEvening))).thenReturn(List.of(tuesdayEveningDto));

        MovieScreeningsDto expectedDto = new MovieScreeningsDto(1L, "Der Name der Rose", List.of());
        when(screeningMapper.toMovieScreeningsDto(eq(movie), anyList())).thenReturn(expectedDto);

        List<MovieScreeningsDto> result = screeningService.findScreeningsOverview();

        assertThat(result).containsExactly(expectedDto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WeekdayScreeningsDto>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
        verify(screeningMapper).toMovieScreeningsDto(eq(movie), scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue()).containsExactly(
                new WeekdayScreeningsDto(Weekday.MO, List.of(mondayAfternoonDto, mondayEveningDto)),
                new WeekdayScreeningsDto(Weekday.DI, List.of(tuesdayEveningDto)));
    }

    @Test
    void ordersMoviesAlphabeticallyByTitle() {
        Movie zMovie = new Movie("Zorro");
        Movie aMovie = new Movie("Alien");
        ReflectionTestUtils.setField(zMovie, "id", 1L);
        ReflectionTestUtils.setField(aMovie, "id", 2L);
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening zScreening = new Screening(zMovie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        Screening aScreening = new Screening(aMovie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        when(screeningRepository.findAllWithMovieAndAuditorium()).thenReturn(List.of(zScreening, aScreening));
        when(screeningMapper.toScreeningDtoList(anyList())).thenReturn(List.of());

        MovieScreeningsDto aDto = new MovieScreeningsDto(2L, "Alien", List.of());
        MovieScreeningsDto zDto = new MovieScreeningsDto(1L, "Zorro", List.of());
        when(screeningMapper.toMovieScreeningsDto(eq(aMovie), anyList())).thenReturn(aDto);
        when(screeningMapper.toMovieScreeningsDto(eq(zMovie), anyList())).thenReturn(zDto);

        List<MovieScreeningsDto> result = screeningService.findScreeningsOverview();

        assertThat(result).containsExactly(aDto, zDto);

        InOrder inOrder = Mockito.inOrder(screeningMapper);
        inOrder.verify(screeningMapper).toMovieScreeningsDto(eq(aMovie), anyList());
        inOrder.verify(screeningMapper).toMovieScreeningsDto(eq(zMovie), anyList());
    }

    @Test
    void returnsEmptyListWhenNoScreeningsExist() {
        when(screeningRepository.findAllWithMovieAndAuditorium()).thenReturn(List.of());

        List<MovieScreeningsDto> result = screeningService.findScreeningsOverview();

        assertThat(result).isEmpty();
        verify(screeningMapper, never()).toMovieScreeningsDto(any(), any());
    }

    @Test
    void returnsScreeningDetailsWithNoBookedSeats() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));
        when(bookingService.findByScreening(screening)).thenReturn(List.of());

        AuditoriumDto auditoriumDto = new AuditoriumDto("Saal 1", List.of());
        when(auditoriumService.getAuditoriumDetails(auditorium, Set.of())).thenReturn(auditoriumDto);

        ScreeningDetailsDto expected = new ScreeningDetailsDto(1L, "Der Name der Rose", Weekday.MO,
                LocalTime.of(17, 0), new BigDecimal("7.00"), auditoriumDto);
        when(screeningMapper.toScreeningDetailsDto(screening, auditoriumDto)).thenReturn(expected);

        ScreeningDetailsDto result = screeningService.getScreeningDetails(1L);

        assertThat(result).isSameAs(expected);
        verify(auditoriumService).getAuditoriumDetails(auditorium, Set.of());
    }

    @Test
    void computesBookedSeatIdsFromExistingBookingsForTheScreening() {
        Movie movie = new Movie("Der Name der Rose");
        Auditorium auditorium = new Auditorium("Saal 1");
        Screening screening = new Screening(movie, auditorium, Weekday.MO, LocalTime.of(17, 0),
                new BigDecimal("7.00"));
        when(screeningRepository.findByIdWithMovieAndAuditorium(1L)).thenReturn(Optional.of(screening));

        Seat bookedSeat = new Seat(auditorium, 1, 1, 0);
        ReflectionTestUtils.setField(bookedSeat, "id", 100L);
        Booking booking = new Booking("B-1", screening, Set.of(bookedSeat), LocalDateTime.now());
        when(bookingService.findByScreening(screening)).thenReturn(List.of(booking));

        AuditoriumDto auditoriumDto = new AuditoriumDto("Saal 1", List.of());
        when(auditoriumService.getAuditoriumDetails(auditorium, Set.of(100L))).thenReturn(auditoriumDto);

        ScreeningDetailsDto expected = new ScreeningDetailsDto(1L, "Der Name der Rose", Weekday.MO,
                LocalTime.of(17, 0), new BigDecimal("7.00"), auditoriumDto);
        when(screeningMapper.toScreeningDetailsDto(screening, auditoriumDto)).thenReturn(expected);

        ScreeningDetailsDto result = screeningService.getScreeningDetails(1L);

        assertThat(result).isSameAs(expected);
        verify(auditoriumService).getAuditoriumDetails(auditorium, Set.of(100L));
    }

    @Test
    void throwsWhenScreeningDoesNotExist() {
        when(screeningRepository.findByIdWithMovieAndAuditorium(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> screeningService.getScreeningDetails(99L))
                .isInstanceOf(ScreeningNotFoundException.class);

        verify(bookingService, never()).findByScreening(any());
        verify(auditoriumService, never()).getAuditoriumDetails(any(), any());
    }
}
