package org.tcc.monitor.jrf.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "dataInicial",
        "dataFinal",
        "observacao",
        "status"
})
@Entity
@Table(name = "ARQUIVOS")
public class ArquivoEntity {
    @Id
    private UUID id;

    @JsonProperty("dataInicial")
    @Column(name = "DATA_GERACAO_INICIAL", nullable = false)
    private LocalDateTime dataGeracaoInicial;

    @JsonProperty("dataFinal")
    @Column(name = "DATA_GERACAO_FINAL")
    private LocalDateTime dataGeracaoFinal;

    @JsonIgnore
    @Column(name = "NOME_ARQUIVO", nullable = false, length = 255)
    private String nomeArquivo;

    @JsonIgnore
    @Column(name = "DIRETORIO_ARQUIVO", nullable = false, columnDefinition = "text")
    private String diretorioArquivo;

    @Column(name = "OBSERVACAO", columnDefinition = "text")
    private String observacao;

    @Column(name = "STATUS", nullable = false, length = 255)
    private String status;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getDataGeracaoInicial() {
        return dataGeracaoInicial;
    }

    public void setDataGeracaoInicial(LocalDateTime dataGeracaoInicial) {
        this.dataGeracaoInicial = dataGeracaoInicial;
    }

    public LocalDateTime getDataGeracaoFinal() {
        return dataGeracaoFinal;
    }

    public void setDataGeracaoFinal(LocalDateTime dataGeracaoFinal) {
        this.dataGeracaoFinal = dataGeracaoFinal;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getDiretorioArquivo() {
        return diretorioArquivo;
    }

    public void setDiretorioArquivo(String diretorioArquivo) {
        this.diretorioArquivo = diretorioArquivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
