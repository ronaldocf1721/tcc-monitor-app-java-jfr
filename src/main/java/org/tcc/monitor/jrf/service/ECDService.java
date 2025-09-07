package org.tcc.monitor.jrf.service;

import org.springframework.stereotype.Service;
import org.tcc.monitor.jrf.dto.*;
import org.tcc.monitor.jrf.entity.ArquivoEntity;
import org.tcc.monitor.jrf.repository.ArquivoRepository;
import org.tcc.monitor.jrf.utils.DateTimeUtils;
import org.tcc.monitor.jrf.utils.DecimalUtils;
import org.tcc.monitor.jrf.utils.DurationUtils;
import org.tcc.monitor.jrf.utils.JdbcUtil;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ECDService {
    private static final String BLOCO_0 = "0";
    private static final String BLOCO_9 = "9";
    private static final String BLOCO_I = "I";
    private static final String BLOCO_K = "K";
    private static final String BLOCO_J = "J";
    private static final String BLOCO_C = "C";
    private static final String REGISTRO_0000 = "0000";
    private static final String REGISTRO_0001 = "0001";
    private static final String REGISTRO_0007 = "0007";
    private static final String REGISTRO_0990 = "0990";
    private static final String REGISTRO_C001 = "C001";
    private static final String REGISTRO_C990 = "C990";
    private static final String REGISTRO_K001 = "K001";
    private static final String REGISTRO_K990 = "K990";
    private static final String REGISTRO_J001 = "J001";
    private static final String REGISTRO_J990 = "J990";
    private static final String REGISTRO_9001 = "9001";
    private static final String REGISTRO_9900 = "9900";
    private static final String REGISTRO_9990 = "9990";
    private static final String REGISTRO_9999 = "9999";
    private static final String REGISTRO_I001 = "I001";
    private static final String REGISTRO_I990 = "I990";
    private static final String REGISTRO_I010 = "I010";
    private static final String REGISTRO_I030 = "I030";
    private static final String REGISTRO_I050 = "I050";
    private static final String REGISTRO_I150 = "I150";
    private static final String REGISTRO_I155 = "I155";
    private static final String REGISTRO_I250 = "I250";
    private static final String REGISTRO_I255 = "I255";
    private static final String DIRETORIO = "arquivos";
    private static final String SEPARADOR = "|";
    private static final String ARQUIVO_NOME = "ECD_%s_%s-%s.txt";
    private static final String LIVRO_DIARIO = "G";
    private static final String COD_VER_LEIAUTE_2020 = "9.00";
    private static final String TERMO_DE_ABERTURA = "TERMO DE ABERTURA";
    private static final String NATUREZA_LIVRO_GERAL = "Diário Geral";
    private static final int TAMANHO_CARAC_REGISTROS = 4;
    private static final String INDICADOR_LANCAMENTO_NORMAL = "N";

    private String nomeArquivo;
    private String diretorio;
    private BufferedWriter arquivo;
    private Parametros parametros;

    private TreeMap<String, Long> qtdLinhasReg = new TreeMap<>();
    private Empresa empresa;

    private final JdbcUtil jdbcUtil;
    private final ArquivoRepository arquivoRepository;

    private ECDService(JdbcUtil jdbcUtil, ArquivoRepository arquivoRepository) {
        this.jdbcUtil = jdbcUtil;
        this.arquivoRepository = arquivoRepository;
        this.parametros = null;
        this.nomeArquivo = null;
        this.diretorio = null;
    }

    public void gerarArquivo(Parametros parametros) throws Exception {
        this.parametros = parametros;

        inicializarRecursos();
        iniciarInfoArquivo();

        salvarObservacaoArquivo("Gerando bloco 0");
        gerarBloco0();
        salvarObservacaoArquivo("Gerando bloco C");
        gerarBlocoC();
        salvarObservacaoArquivo("Gerando bloco I");
        gerarBlocoI();
        salvarObservacaoArquivo("Gerando bloco J");
        gerarBlocoJ();
        salvarObservacaoArquivo("Gerando bloco K");
        gerarBlocoK();
        salvarObservacaoArquivo("Gerando bloco 9");
        gerarBloco9();

        finalizarInfoArquivo();
        liberarRecursos();
    }

    private void iniciarInfoArquivo() {
        ArquivoEntity arquivo = new ArquivoEntity();

        arquivo.setId(UUID.fromString(this.parametros.idRequisicao()));
        arquivo.setStatus("EM_PROCESSAMENTO");
        arquivo.setDataGeracaoInicial(LocalDateTime.now());
        arquivo.setNomeArquivo(this.nomeArquivo);
        arquivo.setDiretorioArquivo(this.diretorio);

        arquivoRepository.save(arquivo);
    }

    private void salvarObservacaoArquivo(String observacao) {
        arquivoRepository.findById(UUID.fromString(this.parametros.idRequisicao())).ifPresent(arquivo -> {
            arquivo.setObservacao(observacao);
            arquivoRepository.save(arquivo);
        });
    }

    private void finalizarInfoArquivo() {
        arquivoRepository.findById(UUID.fromString(this.parametros.idRequisicao())).ifPresent(arquivo -> {
            LocalDateTime dataFinal = LocalDateTime.now();
            arquivo.setDataGeracaoFinal(dataFinal);
            arquivo.setObservacao("Arquivo gerado em: " + DurationUtils.format(Duration.between(arquivo.getDataGeracaoInicial(), dataFinal)));
            arquivo.setStatus("CONCLUIDO");
            arquivoRepository.save(arquivo);
        });
    }

    private void inicializarEmpresa() throws Exception{
        String sql = " SELECT CODIGO, RAZAO_SOCIAL, NOME_FANTASIA, CNPJ, UF, IND_NIRE" + System.lineSeparator() +
                     " FROM EMPRESAS " + System.lineSeparator() +
                     " WHERE CODIGO = ?";

        jdbcUtil.executeQuery(sql,
                stmt -> {
                    try {
                        stmt.setLong(1, this.parametros.codigoEmpresa());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rs -> {
                    try {
                        while (rs.next()) {
                            this.empresa = new Empresa( rs.getInt("CODIGO"),
                                                        rs.getString("RAZAO_SOCIAL"),
                                                        rs.getString("NOME_FANTASIA"),
                                                        rs.getString("CNPJ"),
                                                        rs.getString("UF"),
                                                        rs.getInt("IND_NIRE"));
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private void inicializarRecursos() throws Exception {
        qtdLinhasReg.clear();

        inicializarEmpresa();
        inicializarArquivo();
    }

    private void inicializarArquivo() throws Exception {
        File pasta = new File(DIRETORIO);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        this.nomeArquivo = String.format(ARQUIVO_NOME, empresa.razaoSocial(), DateTimeUtils.formatar(this.parametros.dataInicial()), DateTimeUtils.formatar(this.parametros.dataFinal()));
        this.diretorio = pasta.getAbsolutePath();
        arquivo = new BufferedWriter(new FileWriter(new File(pasta, this.nomeArquivo)));
    }

    private void liberarRecursos() throws IOException {
        arquivo.close();
    }

    private void setValor(String valor) throws IOException {
        arquivo.write(SEPARADOR + valor);
    }
    private void setValorFinal(String valor) throws IOException {
        arquivo.write(SEPARADOR + valor + SEPARADOR);
        arquivo.newLine();
    }

    private void gerarRegistro0000() throws Exception {
        //REG
        setValor(REGISTRO_0000);
        //LECD
        setValor("LECD");
        //DT_INI
        setValor(DateTimeUtils.formatar(this.parametros.dataInicial()));
        //DT_FIN
        setValor(DateTimeUtils.formatar(this.parametros.dataFinal()));
        //NOME
        setValor(this.empresa.razaoSocial());
        //CNPJ
        setValor(this.empresa.cnpj());
        //UF
        setValor(this.empresa.uf());
        //IE
        setValor("");
        //COD_MUN
        setValor("");
        //IM
        setValor("");
        //IND_SIT_ESP
        setValor("");
        //IND_SIT_INI_PER
        setValor("0");
        //IND_NIRE
        setValor(String.valueOf(this.empresa.indNire()));
        //IND_FIN_ESC
        setValor("0");
        //COD_HASH_SUB
        setValor("");
        //IND_GRANDE_PORTE
        setValor("0");
        //TIP_ECD
        setValor("0");
        //COD_SCP
        setValor("");
        //IDENT_MF
        setValor("N");
        //IND_ESC_CONS
        setValor("N");
        //IND_CENTRALIZADA
        setValor("0");
        //IND_MUDANC_PC
        setValor("0");
        //COD_PLAN_REF
        setValorFinal("1");

        incremetarQtdLinhas(REGISTRO_0000);
    }

    private void incremetarQtdLinhas(String registro) {
        long qtd = getQtdLinhas(registro);
        qtd++;
        qtdLinhasReg.put(registro, qtd);

        String bloco = registro.substring(0,1);
        qtd = getQtdLinhas(bloco);
        qtd++;
        qtdLinhasReg.put(bloco, qtd++);
    }

    private void incremetarQtdLinhas(String registro, long qtdCalculada) {
        long qtd = getQtdLinhas(registro);
        qtd += qtdCalculada;
        qtdLinhasReg.put(registro, qtd);

        String bloco = registro.substring(0,1);
        qtd = getQtdLinhas(bloco);
        qtd += qtdCalculada;
        qtdLinhasReg.put(bloco, qtd++);
    }

    private long getQtdLinhas(String chave) {
        return qtdLinhasReg.getOrDefault(chave, 0L);
    }

    private void gerarRegistro0001() throws Exception {
        //REG
        setValor(REGISTRO_0001);
        //IND_DAD
        setValorFinal("0");

        incremetarQtdLinhas(REGISTRO_0001);
    }

    private void gerarRegistro0007() throws Exception {
        //REG
        setValor(REGISTRO_0007);
        //COD_ENT_REF
        setValor("00");
        //COD_INSCR
        setValorFinal("");

        incremetarQtdLinhas(REGISTRO_0007);
    }

    private void gerarRegistro0990() throws Exception {
        incremetarQtdLinhas(REGISTRO_0990);

        //REG
        setValor(REGISTRO_0990);
        //QTD_LIN_0
        setValorFinal(String.valueOf(getQtdLinhas(BLOCO_0)));
    }

    private void gerarBloco0() throws Exception {
        gerarRegistro0000();
        gerarRegistro0001();
        gerarRegistro0007();
        gerarRegistro0990();
    }

    private void gerarBlocoC() throws Exception {
        gerarRegistroC001();
        gerarRegistroC990();
    }

    private void gerarRegistroC001() throws Exception {
        //REG
        setValor(REGISTRO_C001);
        //IND_DAD
        setValorFinal("1");

        incremetarQtdLinhas(REGISTRO_C001);
    }

    private void gerarRegistroC990() throws Exception {
        incremetarQtdLinhas(REGISTRO_C990);

        //REG
        setValor(REGISTRO_C990);
        //QTD_LIN_0
        setValorFinal(String.valueOf(getQtdLinhas(BLOCO_C)));
    }

    private void gerarBlocoI() throws Exception {
        gerarRegistroI001();
        gerarRegistroI010();
        gerarRegistroI030();
        gerarRegistroI050();
        salvarObservacaoArquivo("Gerando registro I150");
        gerarRegistroI150();
        salvarObservacaoArquivo("Gerando registro I250");

        if (utilizarI250Refatorado()) {
            gerarRegistroI250Refatorado();
        } else {
            gerarRegistroI250();
        }

        gerarRegistroI990();
    }

    private boolean utilizarI250Refatorado() {
        return this.parametros.ehI250Refatorado();
    }

    private void gerarRegistroI001() throws Exception {
        //REG
        setValor(REGISTRO_I001);
        //IND_DAD
        setValorFinal("0");

        incremetarQtdLinhas(REGISTRO_I001);
    }

    private void gerarRegistroI010() throws Exception {
        //REG
        setValor(REGISTRO_I010);
        //IND_ESC
        setValor(LIVRO_DIARIO);
        //COD_VER_LC
        setValor(COD_VER_LEIAUTE_2020);


        incremetarQtdLinhas(REGISTRO_I010);
    }

    private void gerarRegistroI030() throws Exception {
        //REG
        setValor(REGISTRO_I030);
        //DNRC_ABERT
        setValor(TERMO_DE_ABERTURA);
        //NUM_ORD
        setValor("1");
        //NAT_LIVR
        setValor(NATUREZA_LIVRO_GERAL);
        //QTD_LIN
        setValor("9999");
        //NOME
        setValor(this.empresa.razaoSocial());
        //NIRE
        setValor("");
        //CNPJ
        setValor(this.empresa.cnpj());
        //DT_ARQ
        setValor("");
        //DT_ARQ_CONV
        setValor("");
        //DESC_MUN
        setValor("");
        //DT_EX_SOCIAL
        setValorFinal(DateTimeUtils.formatar(this.parametros.dataFinal()));

        incremetarQtdLinhas(REGISTRO_I030);
    }

    private void gerarRegistroI050() throws Exception {
        ArrayList contasContabeis = getContasContabeis();

        for (int i = 0; i < contasContabeis.size(); i++) {
            PlanoContas conta = (PlanoContas) contasContabeis.get(i);

            //REG
            setValor(REGISTRO_I050);
            //DT_ALT
            setValor(DateTimeUtils.formatar(conta.dtInicio()));
            //COD_NAT
            setValor("0" + conta.natureza());
            //IND_CTA
            setValor(conta.tipo());
            //NIVEL
            setValor(String.valueOf(conta.nivel()));
            //COD_CTA
            setValor(String.valueOf(conta.codigo()));
            //COD_CTA_SUP
            setValor(conta.contaSuperior());
            //CTA
            setValorFinal(conta.descricao());

            incremetarQtdLinhas(REGISTRO_I050);
        }
    }

    private ArrayList getContasContabeis() {
        ArrayList contasContabeis = new ArrayList<PlanoContas>();

        String sql = " SELECT CODIGO, CONTA, DESCRICAO, DT_INI, DT_FIM, TIPO, CONTA_SUP, NIVEL, NATUREZA" + System.lineSeparator() +
                     " FROM PLANO_CONTAS " + System.lineSeparator() +
                     " ORDER BY CONTA ";

        jdbcUtil.executeQuery(sql,
                null,
                rs -> {
                    try {
                        while (rs.next()) {
                            PlanoContas planoContas = new PlanoContas(  rs.getLong("CODIGO"),
                                                                        rs.getString("CONTA"),
                                                                        rs.getString("DESCRICAO"),
                                                                        DateTimeUtils.toLocalDate(rs.getDate("DT_INI")),
                                                                        DateTimeUtils.toLocalDate(rs.getDate("DT_FIM")),
                                                                        rs.getString("TIPO"),
                                                                        rs.getString("CONTA_SUP"),
                                                                        rs.getInt("NIVEL"),
                                                                        rs.getInt("NATUREZA"));
                            contasContabeis.add(planoContas);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return contasContabeis;
    }

    private void gerarRegistroI150() throws Exception {
        LocalDate dtInicial_tmp = this.parametros.dataInicial();

        while (dtInicial_tmp.isBefore(this.parametros.dataFinal())) {
            LocalDate inicioMes = DateTimeUtils.getPrimerioDiaMes(dtInicial_tmp);
            LocalDate fimMes = DateTimeUtils.getUltimoDiaMes(inicioMes);

            //REG
            setValor(REGISTRO_I150);
            //DT_INI
            setValor(DateTimeUtils.formatar(inicioMes));
            //DT_FIN
            setValorFinal(DateTimeUtils.formatar(fimMes));

            incremetarQtdLinhas(REGISTRO_I150);

            gerarRegistroI155(inicioMes, fimMes);

            dtInicial_tmp = DateTimeUtils.getProximoMes(inicioMes);
        }
    }

    private void gerarRegistroI155(LocalDate dtInicioPeriodo, LocalDate dtFimPeriodo) throws Exception {
        ArrayList saldosPeriodicos = getSaldosPeriodicos(dtInicioPeriodo, dtFimPeriodo);

        for (int i = 0; i < saldosPeriodicos.size(); i++) {
            SaldoPeriodico saldo = (SaldoPeriodico) saldosPeriodicos.get(i);
            //REG
            setValor(REGISTRO_I155);
            //COD_CTA
            setValor(saldo.conta());
            //COD_CCUS
            setValor(saldo.centroCusto());
            //VL_SLD_INI
            setValor(DecimalUtils.formatar(saldo.valorSaldoInicial()));
            //IND_DC_INI
            setValor(saldo.indicadorSaldoInicial());
            //VL_DEB
            setValor(DecimalUtils.formatar(saldo.valorDebito()));
            //VL_CRED
            setValor(DecimalUtils.formatar(saldo.valorCredito()));
            //VL_SLD_FIN
            setValor(DecimalUtils.formatar(saldo.valorSaldoFinal()));
            //IND_DC_FIN
            setValorFinal(saldo.indicadorSaldoFinal());

            incremetarQtdLinhas(REGISTRO_I155);
        }
    }

    private ArrayList getSaldosPeriodicos(LocalDate dtInicioPeriodo, LocalDate dtFimPeriodo) {
        ArrayList saldosPeriodicos = new ArrayList<SaldoPeriodico>();

        String sql =" SELECT  LANC.COD_CTA," + System.lineSeparator() +
                    " 		COALESCE(LANC.VL_SLD_INI,0) AS VL_SLD_INI," + System.lineSeparator() +
                    " 		(CASE  " + System.lineSeparator() +
                    " 			WHEN COALESCE(LANC.VL_SLD_INI,0) < 0 THEN  'D'" + System.lineSeparator() +
                    " 			ELSE 'C'" + System.lineSeparator() +
                    " 		 END) AS IND_DC_INI," + System.lineSeparator() +
                    " 		LANC.VL_DEB," + System.lineSeparator() +
                    " 		LANC.VL_CRED," + System.lineSeparator() +
                    " 		(COALESCE(LANC.VL_SLD_INI,0) + LANC.VL_CRED - LANC.VL_DEB) AS VL_SLD_FIN," + System.lineSeparator() +
                    " 		(CASE  " + System.lineSeparator() +
                    " 			WHEN COALESCE(LANC.VL_SLD_INI,0) + LANC.VL_CRED - LANC.VL_DEB < 0 THEN  'D'" + System.lineSeparator() +
                    " 			ELSE 'C'" + System.lineSeparator() +
                    " 		 END) AS IND_DC_FIN" + System.lineSeparator() +
                    " FROM (" + System.lineSeparator() +
                    " 		SELECT A.COD_CTA," + System.lineSeparator() +
                    " 				SUM(CASE  " + System.lineSeparator() +
                    " 						WHEN A.IND_DC = 'D' THEN A.VL_LCTO" + System.lineSeparator() +
                    " 						ELSE 0" + System.lineSeparator() +
                    " 					END) AS VL_DEB," + System.lineSeparator() +
                    " 				SUM(CASE  " + System.lineSeparator() +
                    " 						WHEN A.IND_DC = 'C' THEN A.VL_LCTO" + System.lineSeparator() +
                    " 						ELSE 0" + System.lineSeparator() +
                    " 					END) AS VL_CRED," + System.lineSeparator() +
                    " 				(SELECT SUM(CASE  " + System.lineSeparator() +
                    " 								WHEN A.IND_DC = 'D' THEN A.VL_LCTO * -1" + System.lineSeparator() +
                    " 								ELSE A.VL_LCTO" + System.lineSeparator() +
                    " 							END)" + System.lineSeparator() +
                    " 				FROM LANC_CONTABEIS A" + System.lineSeparator() +
                    " 				WHERE A.COD_EMP = ?" + System.lineSeparator() +
                    " 				  AND A.DT_LCTO < ?" + System.lineSeparator() +
                    " 				  ) AS VL_SLD_INI" + System.lineSeparator() +
                    " 		FROM LANC_CONTABEIS A" + System.lineSeparator() +
                    " 			INNER JOIN PLANO_CONTAS B ON(B.CODIGO = A.COD_CTA)" + System.lineSeparator() +
                    " 		WHERE A.COD_EMP = ?" + System.lineSeparator() +
                    " 		  AND A.DT_LCTO BETWEEN ? AND ?" + System.lineSeparator() +
                    " 		  AND B.NATUREZA IN(1,2)" + System.lineSeparator() +
                    " 		  AND B.TIPO = 'A'" + System.lineSeparator() +
                    " 		GROUP BY A.COD_CTA" + System.lineSeparator() +
                    " ) AS LANC" + System.lineSeparator() +
                    " ORDER BY LANC.COD_CTA";

        jdbcUtil.executeQuery(sql,
                stmt -> {
                    try {
                        stmt.setLong(1, this.parametros.codigoEmpresa());
                        stmt.setDate(2, Date.valueOf(dtInicioPeriodo));
                        stmt.setLong(3, this.parametros.codigoEmpresa());
                        stmt.setDate(4, Date.valueOf(dtInicioPeriodo));
                        stmt.setDate(5, Date.valueOf(dtFimPeriodo));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rs -> {
                    try {
                        while (rs.next()) {
                            SaldoPeriodico saldoPeriodico = new SaldoPeriodico( String.valueOf(rs.getLong("COD_CTA")),
                                                                                "",
                                                                                rs.getBigDecimal("VL_SLD_INI"),
                                                                                rs.getString("IND_DC_INI"),
                                                                                rs.getBigDecimal("VL_DEB"),
                                                                                rs.getBigDecimal("VL_CRED"),
                                                                                rs.getBigDecimal("VL_SLD_FIN"),
                                                                                rs.getString("IND_DC_FIN"));
                            saldosPeriodicos.add(saldoPeriodico);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return saldosPeriodicos;
    }

    private void gerarRegistroI250Refatorado() throws Exception {
        LancamentoContabilService lancamentoContabilService = new LancamentoContabilService(jdbcUtil);
        lancamentoContabilService.processarLancamentos(this.parametros.codigoEmpresa(), this.parametros.dataInicial(), this.parametros.dataFinal());
        Map<String, LancamentoContabilService.Lancamento> lancamentos = lancamentoContabilService.lancamentos();

        for (LancamentoContabilService.Lancamento lancamento : lancamentos.values()) {
            //REG
            setValor(REGISTRO_I250);
            //NUM_LCTO
            setValor(lancamento.numero());
            //DT_LCTO
            setValor(DateTimeUtils.formatar(lancamento.data()));
            //VL_LCTO
            setValor(DecimalUtils.formatar(lancamento.valor()));
            //IND_LCTO
            setValor(lancamento.indicador());
            //DT_LCTO_EXT
            setValorFinal(lancamento.dataExteporaneo());

            incremetarQtdLinhas(REGISTRO_I250);

            gerarRegistroI255Refatorado(lancamento.partidaDobrada());

        }
    }

    private void gerarRegistroI255Refatorado(ArrayList<LancamentoContabilService.PartidaSimples> partidaDobrada) throws Exception {
        for (int i = 0; i < partidaDobrada.size(); i++) {
            LancamentoContabilService.PartidaSimples lancamento = (LancamentoContabilService.PartidaSimples) partidaDobrada.get(i);

            //REG
            setValor(REGISTRO_I255);
            //COD_CTA
            setValor(lancamento.conta());
            //COD_CCUS
            setValor(lancamento.centroCusto());
            //VL_DC
            setValor(DecimalUtils.formatar(lancamento.valor()));
            //IND_DC
            setValor(lancamento.indicador());
            //NUM_ARQ
            setValor(lancamento.numeroArquivado());
            //COD_HIST_PAD
            setValor(lancamento.historicoPadrao());
            //HIST
            setValor(lancamento.historico());
            //COD_PART
            setValorFinal(lancamento.codigoParticipante());

            incremetarQtdLinhas(REGISTRO_I255);
        }
    }

    private void gerarRegistroI250() throws Exception {
        ArrayList lancamentosContabeis = getLancamentosContabeis();

        for (int i = 0; i < lancamentosContabeis.size(); i++) {
            LancamentoContabil lancamento = (LancamentoContabil) lancamentosContabeis.get(i);
            //REG
            setValor(REGISTRO_I250);
            //NUM_LCTO
            setValor(lancamento.numero());
            //DT_LCTO
            setValor(DateTimeUtils.formatar(lancamento.data()));
            //VL_LCTO
            setValor(DecimalUtils.formatar(lancamento.valor()));
            //IND_LCTO
            setValor(lancamento.indicador());
            //DT_LCTO_EXT
            setValorFinal(lancamento.dataExteporaneo());

            incremetarQtdLinhas(REGISTRO_I250);

            gerarRegistroI255(lancamento.numero(), lancamento.data());
        }
    }

    private ArrayList getLancamentosContabeis() {
        ArrayList lancamentosContabeis = new ArrayList<LancamentoContabil>();

        String sql =" SELECT  A.NUM_LCTO," + System.lineSeparator() +
                    " 		A.DT_LCTO," + System.lineSeparator() +
                    " 		SUM(A.VL_LCTO)/2 AS VL_LCTO " + System.lineSeparator() +
                    " FROM LANC_CONTABEIS A" + System.lineSeparator() +
                    " WHERE A.COD_EMP = ?" + System.lineSeparator() +
                    "   AND A.DT_LCTO BETWEEN ? AND ?" + System.lineSeparator() +
                    " GROUP BY A.NUM_LCTO," + System.lineSeparator() +
                    " 		   A.DT_LCTO" + System.lineSeparator() +
                    " ORDER BY A.DT_LCTO," + System.lineSeparator() +
                    " 		   A.NUM_LCTO";

        jdbcUtil.executeQuery(sql,
                stmt -> {
                    try {
                        stmt.setLong(1, this.parametros.codigoEmpresa());
                        stmt.setDate(2, Date.valueOf(this.parametros.dataInicial()));
                        stmt.setDate(3, Date.valueOf(this.parametros.dataFinal()));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rs -> {
                    try {
                        while (rs.next()) {
                            LancamentoContabil lancamentoContabil = new LancamentoContabil(
                                    String.valueOf(rs.getLong("NUM_LCTO")),
                                    DateTimeUtils.toLocalDate(rs.getTimestamp("DT_LCTO")),
                                    rs.getBigDecimal("VL_LCTO"),
                                    INDICADOR_LANCAMENTO_NORMAL,
                                    "");
                            lancamentosContabeis.add(lancamentoContabil);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return lancamentosContabeis;
    }

    private void gerarRegistroI255(String numeroLancamento, LocalDate dataLancamento) throws Exception {
        ArrayList partidaLancamentosContabeis = getPartidaLancamentosContabeis(numeroLancamento, dataLancamento);

        for (int i = 0; i < partidaLancamentosContabeis.size(); i++) {
            PartidaLancamentoContabil lancamento = (PartidaLancamentoContabil) partidaLancamentosContabeis.get(i);
            //REG
            setValor(REGISTRO_I255);
            //COD_CTA
            setValor(lancamento.conta());
            //COD_CCUS
            setValor(lancamento.centroCusto());
            //VL_DC
            setValor(DecimalUtils.formatar(lancamento.valor()));
            //IND_DC
            setValor(lancamento.indicador());
            //NUM_ARQ
            setValor(lancamento.numeroArquivado());
            //COD_HIST_PAD
            setValor(lancamento.historicoPadrao());
            //HIST
            setValor(lancamento.historico());
            //COD_PART
            setValorFinal(lancamento.codigoParticipante());

            incremetarQtdLinhas(REGISTRO_I255);
        }
    }

    private ArrayList getPartidaLancamentosContabeis(String numeroLancamento, LocalDate dataLancamento) {
        ArrayList partidaLancamentosContabeis = new ArrayList<PartidaLancamentoContabil>();

        String sql =" SELECT  A.COD_CTA," + System.lineSeparator() +
                    " 		A.VL_LCTO," + System.lineSeparator() +
                    " 		A.IND_DC" + System.lineSeparator() +
                    " FROM LANC_CONTABEIS A" + System.lineSeparator() +
                    " WHERE A.COD_EMP = ?" + System.lineSeparator() +
                    "   AND A.DT_LCTO = ?" + System.lineSeparator() +
                    "   AND A.NUM_LCTO = ?" + System.lineSeparator() +
                    " ORDER BY A.COD_CTA," + System.lineSeparator() +
                    " 		 A.IND_DC";

        jdbcUtil.executeQuery(sql,
                stmt -> {
                    try {
                        stmt.setLong(1, this.parametros.codigoEmpresa());
                        stmt.setDate(2, Date.valueOf(dataLancamento));
                        stmt.setLong(3, Long.parseLong(numeroLancamento));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                },
                rs -> {
                    try {
                        while (rs.next()) {
                            PartidaLancamentoContabil partidaLancamentoContabil = new PartidaLancamentoContabil(
                                    String.valueOf(rs.getLong("COD_CTA")),
                                    "",
                                    rs.getBigDecimal("VL_LCTO"),
                                    rs.getString("IND_DC"),
                                    "",
                                    "",
                                    "",
                                    "");
                            partidaLancamentosContabeis.add(partidaLancamentoContabil);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        return partidaLancamentosContabeis;
    }

    private void gerarRegistroI990() throws Exception {
        incremetarQtdLinhas(REGISTRO_I990);

        //REG
        setValor(REGISTRO_I990);
        //QTD_LIN_I
        setValorFinal(String.valueOf(getQtdLinhas(BLOCO_I)));
    }

    private void gerarBlocoK() throws Exception {
        gerarRegistroK001();
        gerarRegistroK990();
    }

    private void gerarRegistroK001() throws Exception {
        //REG
        setValor(REGISTRO_K001);
        //IND_DAD
        setValorFinal("1");

        incremetarQtdLinhas(REGISTRO_K001);
    }

    private void gerarRegistroK990() throws Exception {
        incremetarQtdLinhas(REGISTRO_K990);

        //REG
        setValor(REGISTRO_K990);
        //QTD_LIN_K
        setValorFinal(String.valueOf(getQtdLinhas(BLOCO_K)));
    }

    private void gerarBlocoJ() throws Exception {
        gerarRegistroJ001();
        gerarRegistroJ990();
    }

    private void gerarRegistroJ001() throws Exception {
        //REG
        setValor(REGISTRO_J001);
        //IND_DAD
        setValorFinal("1");

        incremetarQtdLinhas(REGISTRO_J001);
    }

    private void gerarRegistroJ990() throws Exception {
        incremetarQtdLinhas(REGISTRO_J990);

        //REG
        setValor(REGISTRO_J990);
        //QTD_LIN_J
        setValorFinal(String.valueOf(getQtdLinhas(BLOCO_J)));
    }

    private void gerarBloco9() throws Exception {
        gerarRegistro9001();
        gerarRegistro9900();
        gerarRegistro9990();
        gerarRegistro9999();
    }

    private void gerarRegistro9001() throws Exception {
        //REG
        setValor(REGISTRO_9001);
        //IND_DAD
        setValorFinal("0");

        incremetarQtdLinhas(REGISTRO_9001);
    }

    private void gerarRegistro9900() throws Exception {
        long qtdReg9900 = 0;

        for (Map.Entry<String, Long> entry : qtdLinhasReg.entrySet()) {
            String chave = entry.getKey();
            long valor = entry.getValue();

            if (chave.length() == TAMANHO_CARAC_REGISTROS) {
                //REG
                setValor(REGISTRO_9900);
                //REG_BLC
                setValor(chave);
                //QTD_REG_BLC
                setValorFinal(String.valueOf(valor));

                qtdReg9900++;
            }
        }

        incremetarQtdLinhas(REGISTRO_9900, qtdReg9900);
    }

    private void gerarRegistro9990() throws Exception {
        incremetarQtdLinhas(REGISTRO_9990);

        //REG
        setValor(REGISTRO_9990);
        //QTD_LIN_9
        setValorFinal(String.valueOf(getQtdLinhas(BLOCO_9)));
    }

    private void gerarRegistro9999() throws Exception {
        incremetarQtdLinhas(REGISTRO_9999);

        //REG
        setValor(REGISTRO_9999);
        //QTD_LIN
        long qtdLinhaArquivo = getQtdLinhas(BLOCO_0) + getQtdLinhas(BLOCO_9) + getQtdLinhas(BLOCO_I) + getQtdLinhas(BLOCO_K) + getQtdLinhas(BLOCO_J) + getQtdLinhas(BLOCO_C);
        setValorFinal(String.valueOf(qtdLinhaArquivo));
    }
}
