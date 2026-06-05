package ec.edu.espe.banquito.accountservice.dto;

import java.time.LocalDate;

public record EodRequest(
        String requestedBy,
        LocalDate contableDate) {
}
