package ec.edu.espe.banquito.core.accountservice.mapper;

import ec.edu.espe.banquito.core.accountservice.dto.CorrespondentBankDto;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;

public class CorrespondentBankMapper {

    private CorrespondentBankMapper() {
    }

    public static CorrespondentBankDto toDto(CorrespondentBank bank) {
        return new CorrespondentBankDto(
                bank.getBankCode(),
                bank.getBankName(),
                bank.getNostroAccountCode(),
                bank.getVostroAccountCode(),
                bank.getCurrency(),
                bank.getStatus().name(),
                bank.getCreationDate());
    }
}
