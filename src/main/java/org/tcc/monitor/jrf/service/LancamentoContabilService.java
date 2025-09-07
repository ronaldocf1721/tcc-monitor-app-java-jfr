package org.tcc.monitor.jrf.service;

import org.tcc.monitor.jrf.dto.LancamentoContabilInput;
import org.tcc.monitor.jrf.utils.DateTimeUtils;
import org.tcc.monitor.jrf.utils.JdbcUtil;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class LancamentoContabilService {
    private static final String INDICADOR_LANCAMENTO_NORMAL = "N";

    private final JdbcUtil jdbcUtil;
    private Map<String, Lancamento> lancamentos;

    public LancamentoContabilService(JdbcUtil jdbcUtil) {
        this.jdbcUtil = jdbcUtil;
        this.lancamentos = new LinkedHashMap<>();;
    }

    public void processarLancamentos(int codigoEmpresa, LocalDate dataInicial, LocalDate dataFinal){
        String sql =" SELECT  A.NUM_LCTO," + System.lineSeparator() +
                    " 		A.DT_LCTO," + System.lineSeparator() +
                    " 		A.VL_LCTO," + System.lineSeparator() +
                    " 		A.IND_DC," + System.lineSeparator() +
                    " 		A.COD_CTA" + System.lineSeparator() +
                    " FROM LANC_CONTABEIS A" + System.lineSeparator() +
                    " WHERE A.COD_EMP = ?" + System.lineSeparator() +
                    "   AND A.DT_LCTO BETWEEN ? AND ?" + System.lineSeparator() +
                    " ORDER BY A.DT_LCTO," + System.lineSeparator() +
                    " 		 A.NUM_LCTO," + System.lineSeparator() +
                    " 		 A.COD_CTA," + System.lineSeparator() +
                    " 		 A.IND_DC";

        jdbcUtil.executeQuery(sql,
                stmt -> {
                    try {
                        stmt.setLong(1, codigoEmpresa);
                        stmt.setDate(2, Date.valueOf(dataInicial));
                        stmt.setDate(3, Date.valueOf(dataFinal));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rs -> {
                    try {
                        while (rs.next()) {
                            LancamentoContabilInput lancamentoInput = new LancamentoContabilInput(
                                    String.valueOf(rs.getLong("NUM_LCTO")),
                                    DateTimeUtils.toLocalDate(rs.getTimestamp("DT_LCTO")),
                                    rs.getBigDecimal("VL_LCTO"),
                                    rs.getString("IND_DC"),
                                    String.valueOf(rs.getLong("COD_CTA"))
                                    );

                            adicionarLancamento(lancamentoInput);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private void adicionarLancamento(LancamentoContabilInput lancamentoInput) {
        String chave = DateTimeUtils.formatar(lancamentoInput.data()) + lancamentoInput.numero();

        Lancamento lancamento = this.lancamentos.get(chave);

        if (lancamento == null) {
            lancamento = new Lancamento(
                    String.valueOf(lancamentoInput.numero()),
                    lancamentoInput.data(),
                    lancamentoInput.valor(),
                    INDICADOR_LANCAMENTO_NORMAL,
                    "");

            this.lancamentos.put(chave, lancamento);
        }

        lancamento.adicionarPartida(lancamentoInput);

    }

    public Map<String, Lancamento> lancamentos() {
        return this.lancamentos;
    }

    class Lancamento {
        String numero;
        LocalDate data;
        BigDecimal valor;
        String indicador;
        String dataExteporaneo;
        ArrayList<PartidaSimples> partidaDobrada;

        private Lancamento() {}

        public Lancamento(String numero, LocalDate data, BigDecimal valor, String indicador, String dataExteporaneo) {
            this.numero = numero;
            this.data = data;
            this.valor = valor;
            this.indicador = indicador;
            this.dataExteporaneo = dataExteporaneo;
            this.partidaDobrada = new ArrayList<>();
        }

        public String numero() {
            return numero;
        }

        public LocalDate data() {
            return data;
        }

        public BigDecimal valor() {
            return valor;
        }

        public String indicador() {
            return indicador;
        }

        public String dataExteporaneo() {
            return dataExteporaneo;
        }

        public void adicionarPartida(LancamentoContabilInput lancamentoInput) {
            PartidaSimples partidaSimples = new PartidaSimples(
                    String.valueOf(lancamentoInput.conta()),
                    "",
                    lancamentoInput.valor(),
                    lancamentoInput.indicador(),
                    "",
                    "",
                    "",
                    "");

            this.partidaDobrada.add(partidaSimples);

            if (this.partidaDobrada.size() > 1) {
                this.valor = this.valor.add(partidaSimples.valor()).divide(BigDecimal.valueOf(this.partidaDobrada.size()));
            }
        }

        public ArrayList<PartidaSimples> partidaDobrada() {
            return this.partidaDobrada;
        }
    }

    class PartidaSimples {
        String conta;
        String centroCusto;
        BigDecimal valor;
        String indicador;
        String numeroArquivado;
        String historicoPadrao;
        String historico;
        String codigoParticipante;

        private PartidaSimples() {}

        public PartidaSimples(String conta, String centroCusto, BigDecimal valor, String indicador, String numeroArquivado, String historicoPadrao, String historico, String codigoParticipante) {
            this.conta = conta;
            this.centroCusto = centroCusto;
            this.valor = valor;
            this.indicador = indicador;
            this.numeroArquivado = numeroArquivado;
            this.historicoPadrao = historicoPadrao;
            this.historico = historico;
            this.codigoParticipante = codigoParticipante;
        }

        public String conta() {
            return conta;
        }

        public String centroCusto() {
            return centroCusto;
        }

        public BigDecimal valor() {
            return valor;
        }

        public String indicador() {
            return indicador;
        }

        public String numeroArquivado() {
            return numeroArquivado;
        }

        public String historicoPadrao() {
            return historicoPadrao;
        }

        public String historico() {
            return historico;
        }

        public String codigoParticipante() {
            return codigoParticipante;
        }
    }
}
