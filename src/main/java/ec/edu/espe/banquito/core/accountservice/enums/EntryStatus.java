package ec.edu.espe.banquito.core.accountservice.enums;

import lombok.Getter;

@Getter
public enum EntryStatus {
    REGISTRADO("REGISTRADO"),
    ANULADO("ANULADO");

    private final String value;
    EntryStatus(String value) {
        this.value = value;
    }
}
