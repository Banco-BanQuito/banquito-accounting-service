package ec.edu.espe.banquito.accountservice.grpc;

import ec.edu.espe.banquito.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.dto.OperationRequest;
import ec.edu.espe.banquito.accountservice.dto.PostOperationResponse;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingEntryRequest;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingEntryResponse;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingOperationRequest;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingServiceGrpc;
import ec.edu.espe.banquito.accountservice.grpc.proto.JournalLine;
import ec.edu.espe.banquito.accountservice.exception.AccountingException;
import ec.edu.espe.banquito.accountservice.exception.AccountingValidationException;
import ec.edu.espe.banquito.accountservice.service.AccountingRulesService;
import ec.edu.espe.banquito.accountservice.service.AccountingService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
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
                    request.getAccountProductType(),
                    request.getAmount(),
                    request.getCommissionAmount(),
                    request.getReference(),
                    request.getAccountingDate());

            PostOperationResponse result = accountingRulesService.postOperation(dto);
            responseObserver.onNext(toResponse(result));
            responseObserver.onCompleted();
        } catch (AccountingException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
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
}
