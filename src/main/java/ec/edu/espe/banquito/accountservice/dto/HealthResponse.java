package ec.edu.espe.banquito.accountservice.dto;

public record HealthResponse(
        String status,
        String service,
        String version) {
}
