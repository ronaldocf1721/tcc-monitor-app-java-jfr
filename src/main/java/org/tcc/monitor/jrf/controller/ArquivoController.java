package org.tcc.monitor.jrf.controller;

import org.tcc.monitor.jrf.dto.GeracaoArquivoRequisicao;
import org.tcc.monitor.jrf.service.ArquivoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/arquivos-ecd")
public class ArquivoController {

    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> criarArquivo(@RequestBody GeracaoArquivoRequisicao requisicao) throws Exception {
        String identificador = arquivoService.criarArquivo(requisicao);
        return ResponseEntity.ok(Map.of("id", identificador));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> obterStatusArquivo(@PathVariable UUID id) {
        return arquivoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obterArquivo(@PathVariable UUID id) {
        return arquivoService.obterArquivo(id)
                .map(arquivoResponse -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivoResponse.nomeArquivo() + "\"")
                        .contentLength(arquivoResponse.tamanho())
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(arquivoResponse.recurso())
                )
                .orElse(ResponseEntity.notFound().build());
    }

}
