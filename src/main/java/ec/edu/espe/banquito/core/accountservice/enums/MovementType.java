package ec.edu.espe.banquito.core.accountservice.enums;
import lombok.Getter;

@Getter
public enum MovementType {
    DEBITO("DEBITO"),
    CREDITO("CREDITO");

    private final String value;
    MovementType(String value) {
        this.value = value;
    }

    public MovementType opposite() {
        return this == DEBITO ? CREDITO : DEBITO;
    }
}
