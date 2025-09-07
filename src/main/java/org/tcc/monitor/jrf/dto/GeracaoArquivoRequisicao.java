package org.tcc.monitor.jrf.dto;

import java.time.LocalDate;

public record GeracaoArquivoRequisicao(LocalDate dataInicial, LocalDate dataFinal, int codigoEmpresa, boolean ehI250Refatorado) {
}
