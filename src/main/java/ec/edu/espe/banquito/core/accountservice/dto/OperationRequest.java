package ec.edu.espe.banquito.core.accountservice.dto;

public record OperationRequest(
        String operationUuid,
        String operationType,
        String sourceAccountProductType,
        String destinationAccountProductType,
        String amount,
        String commissionAmount,
        String reference,
        String accountingDate,
        String ivaAmount,
        String sourceAccountNumber,
        String destinationAccountNumber,
        String beneficiaryName
) {
    public OperationRequest(
            String operationUuid,
            String operationType,
            String sourceAccountProductType,
            String destinationAccountProductType,
            String amount,
            String commissionAmount,
            String reference,
            String accountingDate,
            String ivaAmount) {
        this(operationUuid, operationType, sourceAccountProductType, destinationAccountProductType, amount,
                commissionAmount, reference, accountingDate, ivaAmount, null, null, null);
    }

    public OperationRequest(
            String operationUuid,
            String operationType,
            String sourceAccountProductType,
            String destinationAccountProductType,
            String amount,
            String commissionAmount,
            String reference,
            String accountingDate) {
        this(operationUuid, operationType, sourceAccountProductType, destinationAccountProductType, amount,
                commissionAmount, reference, accountingDate, null, null, null, null);
    }
}
