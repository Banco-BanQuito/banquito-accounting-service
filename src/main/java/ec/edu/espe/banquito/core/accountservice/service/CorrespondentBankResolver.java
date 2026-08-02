package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.enums.CorrespondentBankStatus;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrespondentBankResolver {

    private final CorrespondentBankRepository correspondentBankRepository;

    public CorrespondentBankResolver(CorrespondentBankRepository correspondentBankRepository) {
        this.correspondentBankRepository = correspondentBankRepository;
    }

    @Transactional(readOnly = true)
    public String resolveNostroAccount(String bankCode) {
        return resolveActiveBank(bankCode).getNostroAccountCode();
    }

    @Transactional(readOnly = true)
    public String resolveVostroAccount(String bankCode) {
        return resolveActiveBank(bankCode).getVostroAccountCode();
    }

    private CorrespondentBank resolveActiveBank(String bankCode) {
        if (bankCode == null || bankCode.isBlank()) {
            throw new AccountingValidationException("bankCode es obligatorio para resolver la cuenta del banco corresponsal.");
        }
        CorrespondentBank bank = correspondentBankRepository.findById(bankCode)
                .orElseThrow(() -> new AccountingValidationException(
                        "El banco corresponsal " + bankCode + " no existe en el catálogo."));
        if (bank.getStatus() != CorrespondentBankStatus.ACTIVO) {
            throw new AccountingValidationException(
                    "El banco corresponsal " + bankCode + " está INACTIVO; no puede recibir asientos.");
        }
        return bank;
    }
}
