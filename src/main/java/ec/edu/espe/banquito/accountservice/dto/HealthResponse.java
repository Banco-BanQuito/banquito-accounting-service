package ec.edu.espe.banquito.accountservice.dto;

/** Respuesta del health check para K8s/Kong. */
public record HealthResponse(
        String status,
        String service,
        String version) {
}
