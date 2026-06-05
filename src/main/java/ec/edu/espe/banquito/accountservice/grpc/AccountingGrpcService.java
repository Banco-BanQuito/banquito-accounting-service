package ec.edu.espe.banquito.accountservice.grpc;

import ec.edu.espe.banquito.accountservice.domain.MovementType;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryLineRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryRequest;
import ec.edu.espe.banquito.accountservice.dto.JournalEntryResponse;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingEntryRequest;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingEntryResponse;
import ec.edu.espe.banquito.accountservice.grpc.proto.AccountingServiceGrpc;
import ec.edu.espe.banquito.accountservice.grpc.proto.JournalLine;
import ec.edu.espe.banquito.accountservice.service.AccountingService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AccountingGrpcService extends AccountingServiceGrpc.AccountingServiceImplBase {

    private final AccountingService accountingService;

    public AccountingGrpcService(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @Override
    public void registerEntry(AccountingEntryRequest request,
                              StreamObserver<AccountingEntryResponse> responseObserver) {
        try {
            JournalEntryResponse result = accountingService.registerEntry(toDto(request));

            responseObserver.onNext(AccountingEntryResponse.newBuilder()
                    .setEntryId(result.entryId())
                    .setEntryUuid(result.entryUuid())
                    .setStatus(result.status())
                    .setValidationResult(result.validationResult())
                    .setRegisteredAt(result.registeredAt().toString())
                    .build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private JournalEntryRequest toDto(AccountingEntryRequest request) {
        LocalDate entryDate = request.getEntryDate().isBlank() ? null : LocalDate.parse(request.getEntryDate());
        List<JournalEntryLineRequest> lines = request.getLinesList().stream().map(this::toLineDto).toList();
        return new JournalEntryRequest(request.getEntryUuid(), request.getDescription(), entryDate, lines);
    }

    private JournalEntryLineRequest toLineDto(JournalLine line) {
        return new JournalEntryLineRequest(
                line.getAccountCode(),
                MovementType.valueOf(line.getMovementType()),
                new BigDecimal(line.getAmount()),
                line.getReference().isBlank() ? null : line.getReference());
    }
}
