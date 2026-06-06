package ec.edu.espe.banquito.accountservice.enums;

public enum MovementType {
    DEBIT,
    CREDIT;

    public static MovementType fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "DEBITO", "DEBIT" -> DEBIT;
            case "CREDITO", "CREDIT" -> CREDIT;
            default -> MovementType.valueOf(value);
        };
    }
}
