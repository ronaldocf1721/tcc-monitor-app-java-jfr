package org.tcc.monitor.jrf.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.tcc.monitor.jrf.dto.ArquivoResponse;
import org.tcc.monitor.jrf.dto.GeracaoArquivoRequisicao;
import org.tcc.monitor.jrf.dto.Parametros;
import org.tcc.monitor.jrf.entity.ArquivoEntity;
import org.tcc.monitor.jrf.repository.ArquivoRepository;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ArquivoService {
    private final ArquivoRepository arquivoRepository;

    @Autowired
    private ECDService ecdService;

    public ArquivoService(ArquivoRepository arquivoRepository) throws IOException {
        this.arquivoRepository = arquivoRepository;
    }

    public Optional<ArquivoEntity> buscarPorId(UUID id) {
        return arquivoRepository.findById(id);
    }

    public String criarArquivo(GeracaoArquivoRequisicao requisicao) {
        String idRequisicao = UUID.randomUUID().toString();

        CompletableFuture.runAsync(() -> {
            try {
                ecdService.gerarArquivo(new Parametros(
                        requisicao.dataInicial(),
                        requisicao.dataFinal(),
                        requisicao.codigoEmpresa(),
                        idRequisicao,
                        requisicao.ehI250Refatorado()
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return idRequisicao;
    }

    public Optional<ArquivoResponse> obterArquivo(UUID id) {
        return buscarPorId(id).flatMap(arquivo -> {
            try {
                Path path = Paths.get(arquivo.getDiretorioArquivo(), arquivo.getNomeArquivo());

                if (!Files.exists(path)) {
                    return Optional.empty();
                }

                InputStream inputStream = new BufferedInputStream(new FileInputStream(path.toFile()));
                InputStreamResource resource = new InputStreamResource(inputStream);

                return Optional.of(new ArquivoResponse(
                        arquivo.getNomeArquivo(),
                        Files.size(path),
                        resource
                ));
            } catch (Exception e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }
}
