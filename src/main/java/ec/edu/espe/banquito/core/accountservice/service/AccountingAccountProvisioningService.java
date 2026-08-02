package ec.edu.espe.banquito.core.accountservice.service;

import ec.edu.espe.banquito.core.accountservice.enums.AccountType;
import ec.edu.espe.banquito.core.accountservice.enums.CorrespondentBankStatus;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.model.AccountingAccount;
import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import ec.edu.espe.banquito.core.accountservice.repository.AccountingAccountRepository;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountingAccountProvisioningService {

    private static final String NOSTRO_PARENT_CODE = "1.1.1.00";
    private static final String VOSTRO_PARENT_CODE = "2.3.0.00";
    private static final String NOSTRO_CLASS = "ACTIVO";
    private static final String VOSTRO_CLASS = "PASIVO";
    private static final String ACCOUNT_SEQUENCE_NAME = "correspondent_bank_account_seq";

    private final CorrespondentBankRepository correspondentBankRepository;
    private final AccountingAccountRepository accountingAccountRepository;
    private final EntityManager entityManager;

    public AccountingAccountProvisioningService(CorrespondentBankRepository correspondentBankRepository,
                                                 AccountingAccountRepository accountingAccountRepository,
                                                 EntityManager entityManager) {
        this.correspondentBankRepository = correspondentBankRepository;
        this.accountingAccountRepository = accountingAccountRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public CorrespondentBank provisionCorrespondentBank(String bankCode, String bankName) {
        if (bankCode == null || bankCode.isBlank()) {
            throw new AccountingValidationException("bankCode es obligatorio.");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new AccountingValidationException("bankName es obligatorio.");
        }
        if (correspondentBankRepository.existsById(bankCode)) {
            throw new AccountingValidationException(
                    "El banco corresponsal " + bankCode + " ya existe en el catálogo.");
        }

        String suffix = nextAccountSuffix();
        String nostroCode = NOSTRO_PARENT_CODE.substring(0, NOSTRO_PARENT_CODE.length() - 2) + suffix;
        String vostroCode = VOSTRO_PARENT_CODE.substring(0, VOSTRO_PARENT_CODE.length() - 2) + suffix;

        accountingAccountRepository.saveAndFlush(
                detailAccount(nostroCode, "Nostro " + bankName, NOSTRO_CLASS, NOSTRO_PARENT_CODE));
        accountingAccountRepository.saveAndFlush(
                detailAccount(vostroCode, "Vostro " + bankName, VOSTRO_CLASS, VOSTRO_PARENT_CODE));

        CorrespondentBank bank = new CorrespondentBank();
        bank.setBankCode(bankCode);
        bank.setBankName(bankName);
        bank.setNostroAccountCode(nostroCode);
        bank.setVostroAccountCode(vostroCode);
        bank.setStatus(CorrespondentBankStatus.ACTIVO);

        return correspondentBankRepository.save(bank);
    }

    private String nextAccountSuffix() {
        Session session = entityManager.unwrap(Session.class);
        SessionFactoryImplementor sessionFactory = session.getSessionFactory().unwrap(SessionFactoryImplementor.class);
        String nextValSql = sessionFactory.getJdbcServices().getDialect().getSequenceSupport()
                .getSequenceNextValString(ACCOUNT_SEQUENCE_NAME);

        Long seqValue = session.createNativeQuery(nextValSql, Long.class).getSingleResult();
        return String.format("%02d", seqValue);
    }

    private AccountingAccount detailAccount(String code, String name, String accountClass, String parentCode) {
        AccountingAccount account = new AccountingAccount();
        account.setAccountCode(code);
        account.setName(name);
        account.setAccountClass(accountClass);
        account.setAccountType(AccountType.DETALLE);
        account.setParentAccountCode(parentCode);
        account.setCurrentBalance(BigDecimal.ZERO);
        return account;
    }
}
