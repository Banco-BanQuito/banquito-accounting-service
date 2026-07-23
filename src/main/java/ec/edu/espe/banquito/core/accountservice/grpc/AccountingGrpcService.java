package ec.edu.espe.banquito.core.accountservice.grpc;

import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryDetailDto;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.core.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.core.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.core.accountservice.dto.PostOperationResponse;
import ec.edu.espe.banquito.core.accountservice.grpc.proto.AccountingEntryRequest;
import ec.edu.espe.banquito.core.accountservice.grpc.proto.AccountingEntryResponse;
import ec.edu.espe.banquito.core.accountservice.grpc.proto.AccountingOperationRequest;
import ec.edu.espe.banquito.core.accountservice.grpc.proto.AccountingServiceGrpc;
import ec.edu.espe.banquito.core.accountservice.grpc.proto.JournalLine;
import ec.edu.espe.banquito.core.accountservice.grpc.proto.ReverseOperationRequest;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingException;
import ec.edu.espe.banquito.core.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.core.accountservice.exception.EntryAlreadyReversedException;
import ec.edu.espe.banquito.core.accountservice.exception.EntryNotFoundException;
import ec.edu.espe.banquito.core.accountservice.exception.UnbalancedEntryException;
import ec.edu.espe.banquito.core.accountservice.service.AccountingRulesService;
import ec.edu.espe.banquito.core.accountservice.service.AccountingService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class AccountingGrpcService extends AccountingServiceGrpc.AccountingServiceImplBase {

    private final AccountingService accountingService;
    private final AccountingRulesService accountingRulesService;

    public AccountingGrpcService(AccountingService accountingService,
                                 AccountingRulesService accountingRulesService) {
        this.accountingService = accountingService;
        this.accountingRulesService = accountingRulesService;
    }

    @Override
    public void registerEntry(AccountingEntryRequest request,
                               StreamObserver<AccountingEntryResponse> responseObserver) {
        try {
            JournalEntryResponse result = accountingService.registerEntry(toEntryDto(request));
            responseObserver.onNext(toResponse(result));
            responseObserver.onCompleted();
        } catch (AccountingException e) {
            responseObserver.onError(toGrpcError(e));
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void postOperation(AccountingOperationRequest request,
                               StreamObserver<AccountingEntryResponse> responseObserver) {
        try {
            OperationRequest dto = new OperationRequest(
                    request.getOperationUuid(),
                    request.getOperationType(),
                    request.getSourceAccountProductType(),
                    request.getDestinationAccountProductType(),
                    request.getAmount(),
                    request.getCommissionAmount(),
                    request.getReference(),
                    request.getAccountingDate(),
                    request.getIvaAmount(),
                    blankToNull(request.getSourceAccountNumber()),
                    blankToNull(request.getDestinationAccountNumber()),
                    blankToNull(request.getBeneficiaryName()));

            PostOperationResponse result = accountingRulesService.postOperation(dto);
            responseObserver.onNext(toResponse(result));
            responseObserver.onCompleted();
        } catch (AccountingException e) {
            responseObserver.onError(toGrpcError(e));
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void reverseOperation(ReverseOperationRequest request,
                                  StreamObserver<AccountingEntryResponse> responseObserver) {
        String entryUuid = request.getEntryUuid();
        log.info("[RF-01][REVERSO] Solicitud de reverso recibida para asiento {}", entryUuid);
        try {
            JournalEntryDetailDto result = accountingService.reverseEntry(entryUuid);
            log.info("[RF-01][REVERSO] Asiento {} reversado correctamente (nuevo asiento {})",
                    entryUuid, result.entryUuid());
            responseObserver.onNext(toResponse(result));
            responseObserver.onCompleted();
        } catch (AccountingException e) {
            log.warn("[RF-01][REVERSO-RECHAZADO] No se pudo reversar el asiento {}: {}", entryUuid, e.getMessage());
            responseObserver.onError(toGrpcError(e));
        } catch (Exception e) {
            log.error("[RF-01][REVERSO-FALLIDO] Error inesperado al reversar el asiento {}: {}",
                    entryUuid, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    /**
     * Traduce cada excepción de dominio a su código gRPC semántico, en vez de
     * colapsar todo en INVALID_ARGUMENT — replica el mismo mapeo que ya usa
     * GlobalExceptionHandler para la API REST (NOT_FOUND / CONFLICT / INVALID_ARGUMENT).
     */
    private io.grpc.StatusRuntimeException toGrpcError(AccountingException e) {
        Status status = switch (e) {
            case EntryNotFoundException ex -> Status.NOT_FOUND;
            case EntryAlreadyReversedException ex -> Status.FAILED_PRECONDITION;
            case UnbalancedEntryException ex -> Status.FAILED_PRECONDITION;
            default -> Status.INVALID_ARGUMENT;
        };
        return status.withDescription(e.getMessage()).asRuntimeException();
    }

    private JournalEntryRequest toEntryDto(AccountingEntryRequest request) {
        LocalDate entryDate = request.getEntryDate().isBlank() ? null : LocalDate.parse(request.getEntryDate());
        List<JournalEntryLineRequest> lines = request.getLinesList().stream().map(this::toLineDto).toList();
        return new JournalEntryRequest(request.getEntryUuid(), request.getDescription(), entryDate, lines);
    }

    private JournalEntryLineRequest toLineDto(JournalLine line) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(line.getAmount());
        } catch (NumberFormatException e) {
            throw new AccountingValidationException("amount de línea no es un número válido: " + line.getAmount());
        }
        return new JournalEntryLineRequest(
                line.getAccountCode(),
                line.getMovementType(),
                amount,
                line.getReference().isBlank() ? null : line.getReference());
    }

    private AccountingEntryResponse toResponse(JournalEntryResponse result) {
        return AccountingEntryResponse.newBuilder()
                .setEntryId(result.entryId())
                .setEntryUuid(result.entryUuid())
                .setStatus(result.status())
                .setValidationResult(result.validationResult())
                .setRegisteredAt(result.registeredAt().toString())
                .build();
    }

    private AccountingEntryResponse toResponse(JournalEntryDetailDto result) {
        return AccountingEntryResponse.newBuilder()
                .setEntryId(result.entryId())
                .setEntryUuid(result.entryUuid())
                .setStatus(result.status())
                .setRegisteredAt(result.entryDate().toString())
                .build();
    }

    private AccountingEntryResponse toResponse(PostOperationResponse result) {
        return AccountingEntryResponse.newBuilder()
                .setEntryId(result.entryId())
                .setEntryUuid(result.entryUuid())
                .setStatus(result.status())
                .setValidationResult(result.validationResult())
                .setRegisteredAt(result.registeredAt().toString())
                .setCommissionAmount(result.commissionAmount().toPlainString())
                .setIvaAmount(result.ivaAmount().toPlainString())
                .setTotalDebited(result.totalDebited().toPlainString())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
