package org.tcc.monitor.jrf.dto;

import java.time.LocalDate;

public record PlanoContas(long codigo, String conta, String descricao, LocalDate dtInicio, LocalDate dtFim, String tipo,
                          String contaSuperior, int nivel, int natureza) {
}
