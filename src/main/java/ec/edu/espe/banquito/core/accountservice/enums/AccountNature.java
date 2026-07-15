package ec.edu.espe.banquito.core.accountservice.enums;

public enum AccountNature {

    DEUDORA,
    ACREEDORA;

    public static AccountNature fromClass(String accountClass) {
        if (accountClass == null) {
            throw new IllegalStateException(
                    "La clase contable es nula; no se puede determinar la naturaleza de la cuenta.");
        }
        String clase = accountClass.trim().toUpperCase();
        switch (clase) {
            case "ACTIVO":
            case "GASTO":
                return DEUDORA;
            case "PASIVO":
            case "PATRIMONIO":
            case "INGRESO":
                return ACREEDORA;
            default:
                throw new IllegalStateException(
                        "Clase contable desconocida: '" + accountClass
                                + "'. Se esperaba ACTIVO, PASIVO, PATRIMONIO, INGRESO o GASTO.");
        }
    }
}
