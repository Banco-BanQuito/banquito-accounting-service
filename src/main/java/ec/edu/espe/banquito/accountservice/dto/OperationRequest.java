package ec.edu.espe.banquito.accountservice.dto;

public record OperationRequest(
        String operationUuid,
        String operationType,
        String accountProductType,
        String amount,
        String commissionAmount,
        String reference,
        String accountingDate
) {}
