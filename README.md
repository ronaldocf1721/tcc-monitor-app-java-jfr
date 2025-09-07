# 📊 TCC - Monitoramento de Aplicação Java com JDK Mission Control (JMC) &  Java Flight Recorder (JFR)

## 📌 Visão Geral
Este projeto foi desenvolvido como parte do Trabalho de Conclusão de Curso (TCC) em Engenharia de Software com objetivo demonstrar o uso de ferramentas de monitoramento em aplicações Java, com foco no **JDK Mission Control (JMC)** e no **Java Flight Recorder (JFR)**.

A aplicação desenvolvida simula uma funcionalidade comum em diversos sistemas **ERP**: a geração do arquivo da **Escrituração Contábil Digital (ECD)**. Esse arquivo, produzido no formato .txt, segue um leiaute padronizado estabelecido pela **Secretaria da Fazenda (SEFAZ)**.

Devido ao **grande volume** de dados envolvidos nesse processo, um dos principais desafios está relacionado ao desempenho da aplicação. Nesse contexto, o projeto propõe uma solução voltada ao monitoramento e à otimização do desempenho, permitindo a análise em tempo real de métricas como uso de CPU, consumo de memória, alocação de objetos e ocorrência de exceções.

---

## ⚙️ Tecnologias Utilizadas
- **Java 17**
- **Spring Boot**
- **Maven**
- **Flyway** (migração de banco de dados)
- **PostgreSQL**
- **Docker / Docker Compose**
- **JDK Mission Control (JMC) & Java Flight Recorder (JFR)** para monitoramento

---

## 📂 Estrutura do Projeto
```
tcc-monitor-app-java-jfr/
├── .env                     # Variáveis de ambiente
├── docker-compose.yml       # Orquestração de containers
├── Dockerfile               # Build da aplicação
├── pom.xml                  # Configuração Maven
├── src/
│   ├── main/java/org/tcc/monitor/jrf/
│   │   ├── Application.java            # Classe principal
│   │   ├── controller/                 # Camada de controle REST
│   │   ├── dto/                        # Objetos de transferência de dados
│   │   ├── entity/                     # Entidades JPA
│   │   ├── repository/                 # Repositórios (Spring Data JPA)
│   │   ├── service/                    # Regras de negócio
│   │   └── utils/                      # Classes utilitárias
│   └── main/resources/
│       ├── application.properties      # Configurações da aplicação
│       └── db/migration/               # Scripts Flyway (SQL)
│           ├── V1__criar_tabelas.sql
│           ├── V2__carga_inicial_empresas.sql
│           ├── V3__carga_inicial_plano_contas.sql
│           └── V4__carga_inicial_lancamentos_contabeis.sql
```

---

## ▶️ Como Executar

### 1) Via Docker Compose (recomendado)
1. Configure o `.env` (já existe um exemplo no projeto):
   ```env
   DB_HOST=postgres
   DB_PORT=5432
   DB_NAME=postgres
   DB_USER=admin
   DB_PASSWORD=admin123
   PGADMIN_USER=admin@admin.com
   PGADMIN_PASSWORD=admin
   ```
2. Suba os serviços:
   ```bash
   docker compose up --build
   ```
   - A API ficará em `http://endereco-servidor:8080`
    - Banco PostgreSQL disponível em: `endereco-servidor:5432`
    - PgAdmin disponível em: `endereco-servidor:5050`
   - O JFR é iniciado automaticamente (ver `JAVA_TOOL_OPTIONS` no `docker-compose.yml`) e grava em `./jfr/app_recording.jfr` (volume montado).  
   - Os arquivos ECD gerados são gravados em `./arquivos` (volume montado).

### 2) Executando localmente (sem Docker)
```bash
# Build
mvn clean package -DskipTests

# Executar com JFR habilitado por 1h
java -XX:StartFlightRecording=duration=1h,filename=./jfr/app_recording.jfr,name=app,settings=profile      -jar target/*.jar
```

---

## 📁 Volumes importantes (Docker)
- `./jfr` ⟶ `/app/jfr` (gravações JFR)  
- `./arquivos` ⟶ `/app/arquivos` (arquivos ECD gerados)

---

## 📚 Funcionalidades Implementadas
- Solicitação para gerar arquivo **ECD** em **.txt**.
- Acompanhamento da geração do arquivo.
- Obter arquivo.
> Para a solicação de geração, foi criado a propriedade **ehI250Refatorado**, ela permite realizar o comparativo de antes e depois da refatoração.

---

## 🔗 Endpoints da API

Base path do recurso: **`/arquivos-ecd`**

> Todos os exemplos abaixo assumem `http://endereco-servidor:8080`.

### 1) Criar solicitação de geração de arquivo ECD
Cria uma nova solicitação e retorna o **ID** correspondente. Como o processo de geração pode levar um tempo considerável, sua execução foi implementada de forma assíncrona.

```
POST /arquivos-ecd
Content-Type: application/json
Accept: application/json
```

**Body**  
```json
{
  "dataInicial": "2024-01-01",
  "dataFinal": "2024-12-31",
  "codigoEmpresa": 1,
  "ehI250Refatorado": false
}
```

**Resposta – 200 OK**
```json
{ "id": "008c6a4d-e832-4ce2-81bd-f730fcad3414" }
```

**Códigos de status**  
- `200 OK` – solicitação aceita e ID retornado.  
- `400 Bad Request` – payload inválido.  
- `500 Internal Server Error` – erro na criação.

---

### 2) Consultar status da solicitação
Retorna o **status** e metadados da solicitação/arquivo.

```
GET /arquivos-ecd/{id}/status
Accept: application/json
```

**Resposta – 200 OK** 

***Status: EM_PROCESSAMENTO***
```json
{
    "id": "008c6a4d-e832-4ce2-81bd-f730fcad3414",
    "dataInicial": "2025-08-18T13:42:35.016977",
    "observacao": "Gerando registro I250",
    "status": "EM_PROCESSAMENTO"
}
```
***Status: CONCLUIDO***
```json
{
    "id": "008c6a4d-e832-4ce2-81bd-f730fcad3414",
    "dataInicial": "2025-08-18T13:42:35.016977",
    "dataFinal": "2025-08-18T13:55:01.83386",
    "observacao": "Arquivo gerado em: 00:12:26.816",
    "status": "CONCLUIDO"
}
```
**Códigos de status**  
- `200 OK` – registro encontrado.  
- `404 Not Found` – ID inexistente.

---

### 3) Baixar o arquivo gerado
Efetua o **download** do arquivo quando disponível.

```
GET /arquivos-ecd/{id}
```

**Resposta – 200 OK**
- **Headers**:  
  - `Content-Disposition: attachment; filename="<nome-do-arquivo>"`  
  - `Content-Type: text/plain`  
  - `Content-Length: <tamanho>`
- **Body**: conteúdo do arquivo.

**Códigos de status**  
- `200 OK` – arquivo retornado.  
- `404 Not Found` – arquivo/ID não encontrado ou ainda não disponível.

---

## 📊 Análise com JMC/JFR
1. Abra o **JDK Mission Control** e carregue `./jfr/app_recording.jfr`.  
2. Navegue pelo **Outline**: *Automated Analysis Results*, *Java Application*, *JVM Internals*, *Environment*.  
3. Use as abas auxiliares: **Flame Graph**, **Stack Trace**, **Results** e **Properties** para detalhar gargalos, pilhas de execução e alertas.

---
