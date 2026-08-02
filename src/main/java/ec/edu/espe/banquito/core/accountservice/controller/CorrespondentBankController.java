package ec.edu.espe.banquito.core.accountservice.controller;

import ec.edu.espe.banquito.core.accountservice.dto.CorrespondentBankDto;
import ec.edu.espe.banquito.core.accountservice.dto.CorrespondentBankRequest;
import ec.edu.espe.banquito.core.accountservice.exception.CorrespondentBankNotFoundException;
import ec.edu.espe.banquito.core.accountservice.mapper.CorrespondentBankMapper;
import ec.edu.espe.banquito.core.accountservice.repository.CorrespondentBankRepository;
import ec.edu.espe.banquito.core.accountservice.service.AccountingAccountProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/accounting/correspondent-banks")
@Tag(name = "CorrespondentBanks", description = "Catálogo de bancos corresponsales y sus cuentas Nostro/Vostro (RF Nostro/Vostro, Fase 1).")
public class CorrespondentBankController {

    private final AccountingAccountProvisioningService provisioningService;
    private final CorrespondentBankRepository correspondentBankRepository;

    public CorrespondentBankController(AccountingAccountProvisioningService provisioningService,
                                       CorrespondentBankRepository correspondentBankRepository) {
        this.provisioningService = provisioningService;
        this.correspondentBankRepository = correspondentBankRepository;
    }

    @PostMapping
    @Operation(summary = "Registrar banco corresponsal", description = "Da de alta un banco corresponsal, provisionando dinámicamente sus cuentas Nostro y Vostro.")
    @ApiResponse(responseCode = "201", description = "Banco corresponsal registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o banco ya existente")
    public ResponseEntity<CorrespondentBankDto> registerCorrespondentBank(@RequestBody CorrespondentBankRequest request) {
        var bank = provisioningService.provisionCorrespondentBank(request.bankCode(), request.bankName());
        return ResponseEntity.status(HttpStatus.CREATED).body(CorrespondentBankMapper.toDto(bank));
    }

    @GetMapping("/{bankCode}")
    @Operation(summary = "Consultar banco corresponsal", description = "Devuelve el catálogo (bancos, cuentas Nostro/Vostro, estado) de un banco corresponsal por su código.")
    @ApiResponse(responseCode = "200", description = "Banco corresponsal encontrado")
    @ApiResponse(responseCode = "404", description = "El banco corresponsal no existe")
    public ResponseEntity<CorrespondentBankDto> getCorrespondentBank(@PathVariable String bankCode) {
        var bank = correspondentBankRepository.findById(bankCode)
                .orElseThrow(() -> new CorrespondentBankNotFoundException(
                        "El banco corresponsal " + bankCode + " no existe en el catálogo."));
        return ResponseEntity.ok(CorrespondentBankMapper.toDto(bank));
    }
}
