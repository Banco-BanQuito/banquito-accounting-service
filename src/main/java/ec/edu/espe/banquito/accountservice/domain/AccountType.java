package ec.edu.espe.banquito.accountservice.domain;

/**
 * Tipo de cuenta dentro del Plan de Cuentas.
 * <ul>
 *   <li>{@code ESTRUCTURAL}: cuenta agrupadora (p.ej. 1.0.0.00 ACTIVOS). No recibe asientos.</li>
 *   <li>{@code DETALLE}: cuenta hoja. Es la única que puede recibir líneas de asiento.</li>
 * </ul>
 */
public enum AccountType {
    ESTRUCTURAL,
    DETALLE
}
