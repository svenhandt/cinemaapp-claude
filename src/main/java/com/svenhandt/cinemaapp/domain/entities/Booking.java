package com.svenhandt.cinemaapp.domain.entities;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "booking")
@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ToString.Include
    @Column(name = "booking_number", nullable = false, unique = true)
    private String bookingNumber;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Getter(AccessLevel.NONE)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_seat",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "seat_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = { "booking_id", "seat_id" })
    )
    private Set<Seat> seats = new LinkedHashSet<>();

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    public Booking(String bookingNumber, Screening screening, Set<Seat> seats, LocalDateTime bookingDate) {
        this.bookingNumber = bookingNumber;
        this.screening = screening;
        this.seats = new LinkedHashSet<>(seats);
        this.bookingDate = bookingDate;
    }

    public Set<Seat> getSeats() {
        return SetUtils.unmodifiableSet(seats);
    }
}
