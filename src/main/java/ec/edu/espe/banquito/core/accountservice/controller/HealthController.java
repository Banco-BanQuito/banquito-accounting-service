package ec.edu.espe.banquito.core.accountservice.controller;

import ec.edu.espe.banquito.core.accountservice.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/accounting")
@Tag(name = "Health", description = "Health check para Kubernetes y Kong.")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Devuelve el estado del servicio.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "accounting-service", "2.0"));
    }
}
