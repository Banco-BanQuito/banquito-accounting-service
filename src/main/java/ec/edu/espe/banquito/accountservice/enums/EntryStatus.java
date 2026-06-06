package ec.edu.espe.banquito.accountservice.enums;

public enum EntryStatus {
    REGISTERED,
    CANCELLED;

    public static EntryStatus fromDatabaseValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "REGISTRADO", "REGISTERED" -> REGISTERED;
            case "ANULADO", "CANCELLED" -> CANCELLED;
            default -> EntryStatus.valueOf(value);
        };
    }
}
