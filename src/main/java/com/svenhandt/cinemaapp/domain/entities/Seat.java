package com.svenhandt.cinemaapp.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "seat",
        uniqueConstraints = @UniqueConstraint(columnNames = { "auditorium_id", "row_number", "seat_number" })
)
@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @ToString.Include
    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @ToString.Include
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "column_position", nullable = false)
    private int columnPosition;

    public Seat(Auditorium auditorium, int rowNumber, int seatNumber, int columnPosition) {
        this.auditorium = auditorium;
        this.rowNumber = rowNumber;
        this.seatNumber = seatNumber;
        this.columnPosition = columnPosition;
    }
}
