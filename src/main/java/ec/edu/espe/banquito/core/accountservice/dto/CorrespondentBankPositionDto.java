package ec.edu.espe.banquito.core.accountservice.dto;

import java.math.BigDecimal;

/**
 * Posición neta de BanQuito frente a un banco corresponsal, para el reporte EOD de riesgo de contraparte (Fase 6).
 *
 * Fórmula de netPosition = nostroBalance − vostroBalance.
 *
 * Nostro (activo/DEUDORA): saldo positivo = fondos que BanQuito ya tiene posicionados en el
 * corresponsal (a nuestro favor). Vostro (pasivo/ACREEDORA): saldo positivo = fondos brutos que
 * el corresponsal nos entregó y BanQuito aún no ha entregado al cliente final (obligación
 * pendiente frente al corresponsal/cliente). Restar Vostro descuenta esa obligación de lo que
 * el corresponsal nos debe.
 *
 * Ejemplo: Nostro = 5,000.00 (fondos nuestros ya posicionados allá) y Vostro = 1,200.00
 * (fondos que el corresponsal nos giró y BanQuito todavía no acredita al cliente destino)
 * → netPosition = 5,000.00 − 1,200.00 = 3,800.00: neto a favor de BanQuito frente a ese banco.
 * Si Vostro fuera 6,000.00 → netPosition = 5,000.00 − 6,000.00 = −1,000.00: neto en contra
 * (BanQuito tiene más obligaciones pendientes de entrega que posición propia en el corresponsal).
 */
public record CorrespondentBankPositionDto(
        String bankCode,
        String bankName,
        String status,
        BigDecimal nostroBalance,
        BigDecimal vostroBalance,
        BigDecimal netPosition) {
}
