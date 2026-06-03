package ec.edu.espe.banquito.accountservice.controller;

import ec.edu.espe.banquito.accountservice.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health check obligatorio para Kubernetes (liveness/readiness) y Kong. */
@RestController
@RequestMapping("/api/v2/accounting")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "accounting-service", "2.0");
    }
}
