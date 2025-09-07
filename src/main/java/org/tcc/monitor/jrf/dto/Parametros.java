package org.tcc.monitor.jrf.dto;

import java.time.LocalDate;

public record Parametros(LocalDate dataInicial, LocalDate dataFinal, int codigoEmpresa, String idRequisicao, boolean ehI250Refatorado) {
}
