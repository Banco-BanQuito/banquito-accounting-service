package ec.edu.espe.banquito.core.accountservice.config;

import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CorrespondentBankInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CorrespondentBankInitializer.class);

    private record SeedBank(String bankCode, String bankName) {
    }

    private static final List<SeedBank> SEED_BANKS = List.of(
            new SeedBank("002", "Banco Pichincha"),
            new SeedBank("010", "Banco de Guayaquil"),
            new SeedBank("017", "Produbanco"),
            new SeedBank("030", "Banco Internacional"),
            new SeedBank("034", "Banco del Austro"),
            new SeedBank("035", "Banco Bolivariano"),
            new SeedBank("037", "Banco del Pacífico"),
            new SeedBank("042", "Banco General Rumiñahui")
    );

    private final CorrespondentBankRepository correspondentBankRepository;
    private final AccountingAccountProvisioningService provisioningService;

    public CorrespondentBankInitializer(CorrespondentBankRepository correspondentBankRepository,
                                        AccountingAccountProvisioningService provisioningService) {
        this.correspondentBankRepository = correspondentBankRepository;
        this.provisioningService = provisioningService;
    }

    @Override
    public void run(String... args) {
        if (correspondentBankRepository.count() > 0) {
            log.info("Catálogo de bancos corresponsales ya cargado; se omite el seed.");
            return;
        }
        for (SeedBank seedBank : SEED_BANKS) {
            provisioningService.provisionCorrespondentBank(seedBank.bankCode(), seedBank.bankName());
        }
        log.info("Bancos corresponsales sembrados: {} bancos.", SEED_BANKS.size());
    }
}
