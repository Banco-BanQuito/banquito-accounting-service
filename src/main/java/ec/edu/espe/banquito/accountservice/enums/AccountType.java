package ec.edu.espe.banquito.accountservice.enums;

public enum AccountType {
    STRUCTURAL,
    DETAIL;

    public static AccountType fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "ESTRUCTURAL", "STRUCTURAL" -> STRUCTURAL;
            case "DETALLE", "DETAIL" -> DETAIL;
            default -> AccountType.valueOf(value);
        };
    }
}
