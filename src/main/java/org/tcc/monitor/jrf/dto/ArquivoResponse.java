package org.tcc.monitor.jrf.dto;

import org.springframework.core.io.InputStreamResource;

public record ArquivoResponse(String nomeArquivo, long tamanho, InputStreamResource recurso) {
}
