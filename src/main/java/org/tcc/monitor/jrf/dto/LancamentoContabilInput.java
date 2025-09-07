package org.tcc.monitor.jrf.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoContabilInput(String numero, LocalDate data, BigDecimal valor, String indicador, String conta) {
}
