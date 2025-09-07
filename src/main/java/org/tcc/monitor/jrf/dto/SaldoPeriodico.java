package org.tcc.monitor.jrf.dto;

import java.math.BigDecimal;

public record SaldoPeriodico(String conta, String centroCusto, BigDecimal valorSaldoInicial,
                             String indicadorSaldoInicial, BigDecimal valorDebito, BigDecimal valorCredito,
                             BigDecimal valorSaldoFinal, String indicadorSaldoFinal) {
}
