package com.svenhandt.cinemaapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentDto(

        @NotBlank(message = "Bitte geben Sie den Karteninhaber ein.")
        String cardholderName,

        @NotBlank(message = "Bitte geben Sie die Kartennummer ein.")
        String cardNumber,

        @NotNull(message = "Bitte geben Sie das Gültigkeitsdatum ein.")
        @Min(value = 1, message = "Bitte geben Sie das Gültigkeitsdatum im Format MM/YY ein.")
        @Max(value = 12, message = "Bitte geben Sie das Gültigkeitsdatum im Format MM/YY ein.")
        Integer expiryMonth,

        @NotNull(message = "Bitte geben Sie das Gültigkeitsdatum ein.")
        Integer expiryYear,

        @NotBlank(message = "Bitte geben Sie die Prüfziffer ein.")
        @Pattern(regexp = "^(\\d{3,4})?$", message = "Die Prüfziffer muss aus 3 oder 4 Ziffern bestehen.")
        String securityCode) {
}
