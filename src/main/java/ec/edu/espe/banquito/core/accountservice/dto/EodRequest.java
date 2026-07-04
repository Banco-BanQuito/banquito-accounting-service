package ec.edu.espe.banquito.core.accountservice.dto;

import java.time.LocalDate;

public record EodRequest(
        String requestedBy,
        LocalDate contableDate) {
}
