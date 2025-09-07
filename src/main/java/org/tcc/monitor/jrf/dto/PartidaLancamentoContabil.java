package org.tcc.monitor.jrf.dto;

import java.math.BigDecimal;

public record PartidaLancamentoContabil(String conta, String centroCusto, BigDecimal valor, String indicador,
                                        String numeroArquivado, String historicoPadrao, String historico,
                                        String codigoParticipante) {
}
