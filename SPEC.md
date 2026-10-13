# SPEC.md

This document defines the **business behaviour** of the cinema ticket application. It is the single source of functional requirements.

Technical architecture, layering, coding standards and testing rules are defined in `CLAUDE.md`. Where the two documents disagree, this document decides *what* the application does and `CLAUDE.md` decides *how* it is built.

Source of the requirements: the Clean Code Developer School application kata *Kinokasse* (<https://ccd-school.de/coding-dojo/application-katas/kinokasse/>).

---

## 1. Purpose & Scope

The application sells cinema tickets online. A visitor walks through four steps:

1. pick a screening from the list of all screenings,
2. choose one or more seats in the seating plan of that screening,
3. enter credit card details,
4. receive a booking confirmation as a PDF.

There is no user account: a visitor is anonymous and a booking is identified solely by its booking number.

Screenings and auditorium layouts originate from text files that are imported once into the database. Bookings are stored in the database and survive an application restart.

---

## 2. Glossary

These terms are binding. Implementations must use exactly these English names for domain concepts.

| Term | German original | Meaning |
| --- | --- | --- |
| `Movie` | Film | A film that is shown, identified by its title. |
| `Screening` | Vorstellung | One showing of a movie: weekday, start time, auditorium and ticket price. |
| `Auditorium` | Saal | A room with a fixed seating layout, identified by its name. |
| `Seat` | Sitzplatz | A single seat in an auditorium, identified by row and seat number. |
| `Booking` | Buchung | A confirmed purchase of one or more seats for exactly one screening. |
| `Payment` | Zahlung | The credit card details captured for a booking. |
| `Weekday` | Wochentag | One of `Mo`, `Di`, `Mi`, `Do`, `Fr`, `Sa`, `So`. |

A `Seat` belongs to an `Auditorium`, never to a `Screening`. Whether a seat is taken is a property of the pair (seat, screening).

---

## 3. Data Sources & Import

All source files are UTF-8 encoded and are read from the classpath resources of the application.

### 3.1 `cinemaapp/Vorstellungen.txt`

One line per movie. Blank lines are ignored.

```
Der Name der Rose; Mo/17:00/Saal 1/7.00,Mo/20:00/Saal 1/8.00,Di/17:00/Saal 2/7.50
```

Structure:

- The movie title, then a semicolon `;`, then a comma-separated list of screenings.
- Each screening consists of four slash-separated fields: `weekday/time/auditorium/price`.
- Whitespace surrounding any separator is insignificant and is trimmed.

Field rules:

| Field | Rule |
| --- | --- |
| title | Non-empty, may contain spaces, must not contain `;`. Must be unique across the file. |
| weekday | Exactly one of `Mo`, `Di`, `Mi`, `Do`, `Fr`, `Sa`, `So`. |
| time | 24-hour clock, `HH:mm`, e.g. `09:30`, `20:00`. |
| auditorium | Non-empty name; an auditorium file of this name must exist (see 3.2). |
| price | Decimal number, `.` as decimal separator, e.g. `7.00`, `7.50`. Currency is EUR. Must be greater than zero. |

A movie must have at least one screening. The same combination of weekday, time and auditorium must not occur twice for the same movie. Different movies may share the same combination of weekday, time and auditorium.

### 3.2 Auditorium files

For every auditorium named in `cinemaapp/Vorstellungen.txt` a file `cinemaapp/<auditorium name>.txt` must exist, 
e.g. `cinemaapp/Saal 1.txt`:

```
    XXXXXXXXXXXXX
  XXXXXXXXXXXXXXXXX
 XXXXXXXXXXXXXXXXXXX
XXXXXXXXXXXXXXXXXXXXX
```

Structure:

- `X` marks a seat. A space marks a gap where there is no seat.
- Each line is one row. The **first line is row 1 and is the row closest to the screen**; row numbers increase towards the back.
- Lines may have different lengths. Trailing whitespace is insignificant.
- A file containing no `X` at all is invalid.

Every seat carries two independent numbers, and both are required:

- **Seat number** — counts only the `X` characters within its row, from left to right, starting at 1. This is the number shown to the visitor and printed on the confirmation. Gaps are not counted, so a row `  XX X` has seats 1, 2 and 3.
- **Column position** — the zero-based index of the character within the line. This is not shown to the visitor; it exists so the seating plan can be rendered with its original indentation and keeps the trapezoid shape of the room.

### 3.3 Import rules

- The import runs at application startup.
- It runs **only when no master data is present yet**. If movies, screenings or auditoriums already exist in the database, the import is skipped entirely.
- The import never deletes or overwrites existing data, and never touches bookings. Restarting the application must not lose a single booking.
- A missing file, an unparsable line or a violated field rule aborts the startup with an error message naming the file and the offending line. A partial import must never be left behind silently.

---

## 4. Functional Requirements

### FR-1 — Screening overview (start page)

**Precondition:** master data has been imported.

The start page lists every movie together with all of its screenings.

**Behaviour:**

The page is presented as a simple cinema website with a classic, minimal browser-style appearance.

At the top of the page there is a header containing:

- A page title: `Kinokasse`
  

Below the header, the main content area contains a list of movies.

Movie Overview:

Each movie is displayed in its own horizontal section.

A movie section consists of:

1. The movie title on the left.
2. A bordered screening-times panel on the right.

The movie title is displayed as plain text, for example:

- `Der Name der Rose`
- `Haare am Rücken`
- `Der Schuh des Königs`

The screening-times panel contains the available screenings grouped by weekday.

The weekdays are displayed in German and in chronological order:

`Mo`, `Di`, `Mi`, `Do`, `Fr`, `Sa`, `So`

Under each weekday, the available screening times are displayed as clickable links.

For example, the screening schedule for `Der Name der Rose` is:

| Mo   | Di   | Mi                 | Do          | Fr                  | Sa          | So   |
|------|------|--------------------|-------------|---------------------|-------------|------|
| 17:00| 17:00| 16:00              | 16:00       | 17:00               | 17:00       | 15:00|
| 20:00|      | 19:00              | 19:00       | 20:00               | 20:00       |      |
|      |      | 22:00              |             | 23:00               |             |      |

Screening Time Links

Each individual screening time is rendered as a link or otherwise interactive element.

The user must be able to select a specific screening by clicking its time. The time is provided by a DTO in the relevant 
controller method.

### FR-2 — Seating plan

**Precondition:** a screening has been selected.

**Behaviour:**

- The page shows movie title, weekday, start time, auditorium name and the ticket price of the selected screening.
  - The seating plan renders the auditorium layout, preserving gaps and indentation so the shape of the room is recognisable.
    - example:
  ```
          XXXXXXXXXXXXX
        XXXXXXXXXXXXXXXXX
       XXXXXXXXXXXXXXXXXXX
      XXXXXXXXXXXXXXXXXXXXX
  ```
  an X should be rendered as a checkbox.
- To the left of the seating plan, each row is labelled with its row number (row 1 is closest to the screen). The row-number labels line up in a single vertical column, independent of each row's own indentation.
- Each seat has one of three states:
  - **available** — selectable,
  - **booked** — rendered grey and not selectable,
  - **selected** — chosen by the visitor and selectable again to deselect.
- A seat is *booked* when it is part of an existing booking **for this screening**. The same seat is independently available for every other screening.
- Every change to the selection immediately recalculates and displays the total (see BR-1).
- The *Book* button is disabled while no seat is selected and becomes enabled as soon as at least one seat is selected.
- Submitting leads to the payment dialog (FR-3) carrying the selected screening and seats.

**Acceptance criteria:**

- A seat belonging to an existing booking for this screening is grey and cannot be selected.
- The same seat is available in the seating plan of a different screening in the same auditorium.
- Selecting two seats at a price of `7.50` shows a total of `15.00`; deselecting one shows `7.50`.
- With zero seats selected the *Book* button is disabled.

**Technical details:**
- a click on a start time of a screening triggers a method "getScreeningDetails" in the ScreeningsController class.
- getScreeningDetails listens to the path /screenings/{screeningId}.
- getScreeningDetails invokes the ScreeningsService.getScreeningDetails method. This method collects information about the Screening via its
  Screening entity object.
- to get Details about the auditorium, and its seat arrangement, the ScreeningsService.getScreeningDetails method invokes the
  AuditoriumService.getAuditoriumDetails method, which collects similary to the ScreeningService information about the Auditorium.
- to keep thing decoupled, AuditoriumService.getAuditoriumDetails collects informations about its Seats from the SeatService. SeatService
  will contain a method named getSeats(Auditorium auditorium), which of course calls the respective SeatRepository method.
- ScreeningsService.getScreeningDetails, AuditoriumService.getAuditoriumDetails and SeatService.getSeats shall all return DTOs.
  Hereby: ScreeningsService.getScreeningDetails shall return a ScreeningDTO, AuditoriumService.getAuditoriumDetails should return an AuditoriumDTO,
  and SeatService.getSeats shall return a list of SeatDTOs.
- A ScreeningDTO contains an AuditoriumDTO.
- An AuditoriumDTO contains a list of SeatDTOs.
- A SeatDTO reflects the Seat entity.

### FR-3 — Payment dialog

**Precondition:** at least one seat is selected for a screening.

**Behaviour:**

- The dialog shows a summary of the pending purchase: movie, weekday, time, auditorium, the selected seats and the total.
- It captures four credit card fields: **cardholder name**, **card number**, **expiry date** (`MM/YY`) and **security code**.
- The expiry date field and the security code field are displayed side by side: the expiry date field is left-aligned and the security code field is right-aligned with the card number field.
- The *Continue* button is disabled until all four fields contain input.
- On submission the input is validated (see section 6). The card number must additionally satisfy the Luhn checksum (BR-3).
- If validation fails, the dialog is redisplayed with the entered values preserved — except the security code — and a message per offending field. No booking is created.
- If validation succeeds, the booking is created and the visitor reaches the confirmation (FR-4).

**Acceptance criteria:**

- Submitting with a card number that fails the Luhn check redisplays the dialog with the card number message and creates no booking.
- Submitting with an empty cardholder name redisplays the dialog with the corresponding message.
- A successful submission creates exactly one booking containing exactly the selected seats.

### FR-4 — Booking confirmation

**Precondition:** payment data has been accepted.

**Behaviour:**

- The booking is persisted with a unique booking number (BR-4) and the seats become booked for that screening.
- The confirmation page shows the booking number and the full summary, and offers the confirmation as a PDF.
- The PDF is delivered as a download and contains all relevant data of the booking:
  - booking number,
  - movie title,
  - weekday and start time,
  - auditorium name,
  - every booked seat as row and seat number,
  - the price per seat,
  - the total,
  - the date of the booking.
- The PDF of an existing booking can be retrieved again via its booking number; retrieving it does not change the booking.
- Credit card details never appear in the PDF.

**Acceptance criteria:**

- The generated file is a valid PDF and contains the booking number and every booked seat.
- The total in the PDF equals the total shown in the seating plan.
- Requesting the PDF of an unknown booking number yields a "not found" response, not an error page.

---

## 5. Business Rules

### BR-1 — Pricing

The ticket price belongs to the screening and is identical for every seat in it. There are no discounts, categories or surcharges.

```
total = number of selected seats × ticket price of the screening
```

Amounts are in EUR and are displayed with exactly two decimal places.

### BR-2 — Seat availability

A seat can be booked **at most once per screening**. Availability is validated on the server when the booking is created, not only in the view — the state shown in the seating plan is never the authority.

A seat that is booked for one screening stays available for all other screenings in the same auditorium.

### BR-3 — Card number checksum (Luhn)

The card number must satisfy the Luhn checksum. Before validation, spaces are removed from the input; afterwards the number must consist of digits only and be between 13 and 19 digits long.

The algorithm:

1. Starting from the rightmost digit, double every second digit.
2. If a doubled value is greater than 9, subtract 9 from it.
3. Sum all resulting digits.
4. The number is valid when that sum is divisible by 10.

Examples:

| Card number | Valid |
| --- | --- |
| `4539148803436467` | yes |
| `4539 1488 0343 6467` | yes — spaces are removed before checking |
| `4539148803436468` | no — checksum fails |
| `45391488034364A7` | no — contains a non-digit |

The check verifies the *structure* of the number only. No card is contacted, no money is moved, and no card data is stored beyond what the booking needs.

### BR-4 — Booking number

Every booking receives a booking number that is unique across all bookings, is generated when the booking is created, is shown to the visitor, and never changes afterwards. It is the only means by which a visitor can refer to a booking.

---

## 6. Validation & Error Messages

Feldbezogene Validierung des Zahlungsdialogs (FR-3). Alle Meldungen werden neben dem betroffenen Feld angezeigt.

| Field | Rule | Message |
| --- | --- | --- |
| Cardholder name | Must not be empty | `Bitte geben Sie den Karteninhaber ein.` |
| Card number | Must not be empty | `Bitte geben Sie die Kartennummer ein.` |
| Card number | 13–19 digits after removing spaces | `Die Kartennummer muss aus 13 bis 19 Ziffern bestehen.` |
| Card number | Luhn checksum must pass (BR-3) | `Die Kartennummer ist ungültig.` |
| Expiry date | Must not be empty | `Bitte geben Sie das Gültigkeitsdatum ein.` |
| Expiry date | Format `MM/YY`, month `01`–`12` | `Bitte geben Sie das Gültigkeitsdatum im Format MM/YY ein.` |
| Security code | Must not be empty | `Bitte geben Sie die Prüfziffer ein.` |
| Security code | 3 or 4 digits | `Die Prüfziffer muss aus 3 oder 4 Ziffern bestehen.` |

Validierung der Sitzplatzauswahl (FR-2, FR-4):

| Situation | Message |
| --- | --- |
| Booking submitted with no seat selected | `Bitte wählen Sie mindestens einen Sitzplatz aus.` |
| A selected seat is already booked for this screening | `Der Sitzplatz ist nicht mehr verfügbar.` |
| Screening referenced by the request does not exist | `Die Vorstellung existiert nicht.` |

---

## 7. Out of Scope

The following is deliberately not part of this specification:

- user accounts, login, registration,
- real payment processing — no card is charged and no payment provider is contacted,
- cancelling, changing or refunding a booking,
- time-limited seat reservation while the visitor is choosing,
- checking whether the expiry date of the card lies in the past — only the *format* of the expiry date is validated (see section 6),
- a localised user interface,
- seat categories, discounts, loyalty schemes,
- an administrative interface for maintaining movies, screenings or auditoriums; master data comes exclusively from the import (section 3).

### Known gap

The behaviour when a seat becomes booked by someone else **while a visitor is still on the seating plan** is intentionally left unspecified. BR-2 requires the server to reject such a booking, but the resulting user journey — what the visitor sees and where they continue — has not been defined. This should be specified before the application is used by more than one visitor at a time.

---

## 8. Traceability

Every statement of the original kata maps to a requirement here.

| Kata statement | Requirement |
| --- | --- |
| Application for buying cinema tickets online | Section 1 |
| Start page shows all screenings in a standardised format | FR-1 |
| Clicking a time selects movie, weekday and time | FR-1 |
| Seating plan for choosing seats | FR-2 |
| Booked seats are grey and not selectable | FR-2, BR-2 |
| Total is recalculated on every change of the selection | FR-2, BR-1 |
| *Book* is enabled once at least one seat is selected | FR-2 |
| Payment dialog captures credit card information | FR-3 |
| *Continue* is enabled once all data is entered | FR-3 |
| A PDF with all relevant data is generated | FR-4 |
| Screenings are loaded from `Vorstellungen.txt` | Section 3.1 |
| Weekday, time, auditorium and price per entry | Section 3.1 |
| One file per auditorium, named `<auditorium>.txt` | Section 3.2 |
| A seat is denoted by `X` | Section 3.2 |
| *Variation:* card number checked via the Luhn algorithm | BR-3 |
