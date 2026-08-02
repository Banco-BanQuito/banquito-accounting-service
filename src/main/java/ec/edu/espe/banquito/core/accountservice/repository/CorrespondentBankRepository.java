package ec.edu.espe.banquito.core.accountservice.repository;

import ec.edu.espe.banquito.core.accountservice.model.CorrespondentBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrespondentBankRepository extends JpaRepository<CorrespondentBank, String> {

    List<CorrespondentBank> findAllByOrderByBankCodeAsc();
}
