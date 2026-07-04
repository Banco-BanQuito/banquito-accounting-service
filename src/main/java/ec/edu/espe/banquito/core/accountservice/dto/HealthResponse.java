package ec.edu.espe.banquito.core.accountservice.dto;

public record HealthResponse(
        String status,
        String service,
        String version) {
}
