package ec.edu.espe.banquito.accountservice.dto;

public record OperationRequest(
        String operationUuid,
        String operationType,
        String sourceAccountProductType,
        String destinationAccountProductType,
        String amount,
        String commissionAmount,
        String reference,
        String accountingDate
) {}
