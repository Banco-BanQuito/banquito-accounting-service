package ec.edu.espe.banquito.core.accountservice.enums;

/**
 * NATURALEZA DE UNA CUENTA CONTABLE.
 *
 * <p>En contabilidad cada cuenta tiene un "lado normal" donde su saldo CRECE. A eso se le
 * llama la "naturaleza" de la cuenta, y depende de a qué familia (clase) pertenece:</p>
 *
 * <ul>
 *   <li><b>DEUDORA</b>: las cuentas de ACTIVO (lo que el banco TIENE, como el efectivo)
 *       y de GASTO (lo que el banco GASTA). Estas cuentas SUBEN cuando se DEBITAN.</li>
 *   <li><b>ACREEDORA</b>: las cuentas de PASIVO (lo que el banco DEBE, como el dinero de
 *       los clientes), de PATRIMONIO (el capital propio del banco) y de INGRESO (lo que el
 *       banco GANA). Estas cuentas SUBEN cuando se ACREDITAN.</li>
 * </ul>
 *
 * <p>Regla mnemotécnica: "una cuenta sube por su propio lado". Una cuenta DEUDORA sube al
 * debitar; una cuenta ACREEDORA sube al acreditar.</p>
 */
public enum AccountNature {

    /** ACTIVO y GASTO: suben al DEBITAR. */
    DEUDORA,

    /** PASIVO, PATRIMONIO e INGRESO: suben al ACREDITAR. */
    ACREEDORA;

    /**
     * Averigua la naturaleza de una cuenta a partir del texto de su clase contable.
     *
     * <p>Acepta el texto sin importar mayúsculas o minúsculas (por ejemplo "activo" o
     * "ACTIVO" dan lo mismo). Estas son las mismas clases que usa el Plan de Cuentas
     * sembrado: ACTIVO, PASIVO, PATRIMONIO, INGRESO y GASTO.</p>
     *
     * <ul>
     *   <li>ACTIVO, GASTO -> DEUDORA</li>
     *   <li>PASIVO, PATRIMONIO, INGRESO -> ACREEDORA</li>
     * </ul>
     *
     * <p>Si llega una clase desconocida, se lanza un error de inmediato (fail fast) en lugar
     * de adivinar; así un dato mal cargado se detecta cuanto antes.</p>
     *
     * @param accountClass texto de la clase contable (ej. "ACTIVO"); no debe ser nulo
     * @return la naturaleza que corresponde a esa clase
     * @throws IllegalStateException si la clase es nula o no es una de las clases conocidas
     */
    public static AccountNature fromClass(String accountClass) {
        if (accountClass == null) {
            throw new IllegalStateException(
                    "La clase contable es nula; no se puede determinar la naturaleza de la cuenta.");
        }
        // Normalizamos a mayúsculas para comparar sin importar cómo se escribió el texto.
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
