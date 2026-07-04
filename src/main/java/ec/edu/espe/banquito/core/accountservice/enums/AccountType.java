package ec.edu.espe.banquito.core.accountservice.enums;
import lombok.Getter;

@Getter

public enum AccountType {
    ESTRUCTURAL("ESTRUCTURAL"),
    DETALLE("DETALLE");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

}
