package ec.edu.espe.banquito.core.accountservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void healthReturnsUpStatus() {
        ResponseEntity<?> response = new HealthController().health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
