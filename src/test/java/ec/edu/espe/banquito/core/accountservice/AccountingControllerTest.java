package ec.edu.espe.banquito.core.accountservice;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.espe.banquito.core.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import ec.edu.espe.banquito.core.accountservice.service.AccountingRulesService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * @AutoConfigureMockMvc genera una clave de cache de ApplicationContext distinta a la de los
 * demás @SpringBootTest de este módulo (que no lo usan), por lo que Spring levanta un segundo
 * contexto independiente. Como src/test/resources/application.properties usa una BD H2 en
 * memoria con nombre fijo y DB_CLOSE_DELAY=-1 (para persistir entre los tests que sí comparten
 * contexto), ese segundo contexto reejecutaría schema.sql sobre la misma base ya inicializada
 * y fallaría con "tabla ya existe". Se apunta este contexto a su propia BD H2 nombrada para
 * evitar la colisión, sin tocar la configuración compartida por el resto de la suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:accountingdb_controllertest;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class AccountingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountingAccountProvisioningService provisioningService;

    @Autowired
    private AccountingRulesService rulesService;

    @Test
    void consultaPosicionPorBancoCorresponsalConFechaValida() throws Exception {
        CorrespondentBank pichincha = provisioningService.provisionCorrespondentBank("002", "Banco Pichincha");
        rulesService.postOperation(new OperationRequest(
                "uuid-ctrl-pos-001", "NOSTRO_SETTLEMENT_OUTBOUND", null, null,
                new BigDecimal("250.00").toPlainString(), null, "NOUT-CTRL-001", null, null, pichincha.getBankCode()));

        mockMvc.perform(get("/api/v2/accounting/reports/correspondent-position")
                        .param("date", "2026-08-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bankCode").value("002"))
                .andExpect(jsonPath("$[0].nostroBalance").value(250.00))
                .andExpect(jsonPath("$[0].netPosition").value(250.00));
    }

    @Test
    void consultaPosicionPorBancoCorresponsalSinFechaUsaFechaContableActiva() throws Exception {
        mockMvc.perform(get("/api/v2/accounting/reports/correspondent-position"))
                .andExpect(status().isOk());
    }

    @Test
    void consultaPosicionPorBancoCorresponsalRechazaFechaMalFormada() throws Exception {
        mockMvc.perform(get("/api/v2/accounting/reports/correspondent-position")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
    }
}
