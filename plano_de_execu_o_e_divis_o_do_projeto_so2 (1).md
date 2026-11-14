# Plano de Execução e Divisão do Projeto — Linha de Produção Concorrente (SO II)

**Integrantes:** Fabrício, Eduardo e Yago  
**Tecnologia:** Java (JDK 17 ou 21) — Código puro, sem pacotes (tudo na mesma pasta para compilar fácil com `javac *.java` e rodar com `java Fabrica`).  
**Objetivo:** Implementar a simulação da fábrica com semáforos reais (`java.util.concurrent.Semaphore`), buffers limitados e encerramento coordenado, dividindo o projeto em **3 partes lógicas equilibradas** para facilitar tanto a geração por IA (no Antigravity) quanto a sustentação oral.

---

## 1. Visão Geral da Divisão (Equilíbrio de Trabalho e Sustentação)

Para que todos trabalhem de forma sequencial no Git sem conflitos e cada um tenha uma parte clara para defender na apresentação, o projeto será dividido em **3 módulos independentes**:

1. **Parte 1 — Fabrício (Base, Peça e Buffer Concorrente com Semáforos):** Cria a estrutura de dados da peça, as configurações globais e o coração de Sistemas Operacionais do projeto: a fila limitada controlada pelos 3 semáforos (`vagas`, `itens`, `mutex`).
2. **Parte 2 — Yago (Lógica das Máquinas, Exaustor, Inspeção e Embalagem):** Cria as threads consumidoras/produtoras (as máquinas da fábrica), a disputa pelo recurso compartilhado (exaustor entre Soldagem e Pintura), a regra de aprovação/descarte na Inspeção e a lógica de fechar caixas de 4 peças na Embalagem.
3. **Parte 3 — Eduardo (Orquestração da Fábrica, Encerramento `FIM`, Relatório e Testes):** Cria o programa principal (`Fabrica`), instancia e conecta as filas às 8 máquinas, gera as peças, executa o protocolo obrigatório de encerramento em cascata (`FIM` + `join()`) e gera o relatório final com os 5 cenários de teste.

---

## 2. Detalhamento Técnico por Integrante

### PARTE 1: Fabrício — Estruturas Base e Sincronização das Filas
**Arquivos que o Fabrício vai criar:** `Configuracao.java`, `Peca.java`, `FilaLimitada.java` e `Relatorio.java` (estrutura base de log).

* **`Configuracao.java`**:
  * Reúne todas as constantes que o professor exige que sejam alteráveis: quantidade de peças (`24`), intervalo de chegada (`50ms`), fator de tempo (`1.0`), capacidades das filas (`Corte=5, Furacao=3, Soldagem=3, Pintura=3, Inspecao=4, Embalagem=4`) e durações de cada máquina.
  * Método auxiliar para calcular pausa: `Math.max(1, Math.round(ms * fatorTempo))`.
* **`Peca.java`**:
  * Objeto simples (não é thread!).
  * Atributos: `int id`, `boolean fim` (quando for o marcador de encerramento, `id = -1` e `fim = true`), `boolean aprovada`, `int numeroCaixa` e uma lista de Strings `historico` para guardar a ordem exata das etapas pelas quais passou.
* **`FilaLimitada.java`** *(Ponto central da avaliação de SO)*:
  * Usa um `ArrayDeque<Peca>` interno. **Proibido usar `BlockingQueue`**.
  * Possui os 3 semáforos obrigatórios:
    * `Semaphore vagas = new Semaphore(capacidade);`
    * `Semaphore itens = new Semaphore(0);`
    * `Semaphore mutex = new Semaphore(1);`
  * Guarda `ocupacaoAtual` e `maiorOcupacao` (atualizados **sempre** dentro do `mutex`).
  * **Método `colocar(Peca p)`**:
    1. `vagas.acquire()` (bloqueia fora do mutex se estiver cheia).
    2. `mutex.acquire()`
    3. `fila.addLast(p)`, incrementa `ocupacaoAtual` e atualiza `maiorOcupacao`.
    4. `mutex.release()` (dentro de bloco `finally`).
    5. `itens.release()`
  * **Método `retirar()`**:
    1. `itens.acquire()` (bloqueia fora do mutex se estiver vazia).
    2. `mutex.acquire()`
    3. `Peca p = fila.removeFirst()`, decrementa `ocupacaoAtual`.
    4. `mutex.release()` (dentro de bloco `finally`).
    5. `vagas.release()`
    6. Retorna `p`.
* **`Relatorio.java`**:
  * Classe compartilhada com um `Semaphore mutexRelatorio = new Semaphore(1)` (ou métodos `synchronized`) para imprimir logs no console sem misturar linhas (`[Tempo ms] [Maquina] [Peca ID] Evento`) e guardar os contadores finais (peças por máquina, IDs aprovados, IDs descartados e listas de caixas fechadas).

**O que o Fabrício vai explicar na apresentação:**
* Como funcionam os 3 semáforos da `FilaLimitada`.
* Por que o `mutex` sozinho não resolve (não bloqueia fila cheia/vazia) e por que dar `acquire()` em `vagas`/`itens` **antes** do `mutex` evita *deadlock*.

---

### PARTE 2: Yago — Threads das Máquinas, Exaustor e Embalagem
**Arquivos que o Yago vai criar:** `Maquina.java` e `MaquinaEmbalagem.java`.

* **`Maquina.java` (implementa `Runnable`)**:
  * Representa as máquinas `C1, C2, F1, S1, P1, I1, I2`.
  * Recebe no construtor: nome (`"C1"`), nome do setor (`"Corte"`), `FilaLimitada filaEntrada`, `FilaLimitada filaSaida`, `int duracaoMs`, `Semaphore exaustor` (recebe o semáforo real apenas em `S1` e `P1`; nas demais recebe `null`), e a referência do `Relatorio`.
  * **Lógica do `run()` (loop `while(true)`)**:
    1. Retira peça da `filaEntrada`.
    2. **Teste de parada:** Se `peca.isFim() == true`, sai do laço (`break`) e encerra a thread sem processar e sem repassar o `FIM` adiante.
    3. **Recurso Auxiliar (Exaustor):** Se `exaustor != null` (`S1` ou `P1`), faz `exaustor.acquire()` e registra no log `"ADQUIRIU EXAUSTOR"`.
    4. Registra `"INICIO OPERACAO"` e simula o trabalho com `Thread.sleep(Configuracao.tempo(duracaoMs))` (fora de qualquer região crítica de fila!).
    5. Registra `"FIM OPERACAO"` e adiciona a etapa no histórico da peça.
    6. **Liberação do Exaustor:** Se `exaustor != null`, faz `exaustor.release()` e registra `"LIBEROU EXAUSTOR"`. *(Regra de ouro do PDF: liberar o exaustor ANTES de tentar colocar a peça na próxima fila!)*
    7. **Inspeção ou Encaminhamento:**
       * Se a máquina for de Inspeção (`I1` ou `I2`): verifica `if (peca.id % 7 == 0)`. Se for divisível por 7, marca como reprovada, registra no `Relatorio` como descartada e **não** envia para a `filaSaida`. Se não for divisível por 7, marca como aprovada, registra no `Relatorio` e envia para a `filaSaida` (Embalagem).
       * Para todas as outras máquinas: registra `"AGUARDANDO SAIDA"` e chama `filaSaida.colocar(peca)`.
* **`MaquinaEmbalagem.java` (implementa `Runnable`)**:
  * Representa a máquina `E1`.
  * Possui uma lista interna privada `List<Integer> caixaAberta = new ArrayList<>()`.
  * No `run()`:
    1. Retira peça da `filaEmbalagem`.
    2. **Se for `FIM`**: verifica se `caixaAberta` tem peças (`!caixaAberta.isEmpty()`). Se tiver (1 a 3 peças), faz `Thread.sleep(tempoFechamentoCaixa)`, registra o fechamento da **caixa parcial** no `Relatorio` e encerra a thread. Se estiver vazia, apenas encerra.
    3. **Se for peça normal**: dorme `150ms` (acomodação da peça), adiciona o ID na `caixaAberta` e contabiliza a peça.
    4. **Se `caixaAberta.size() == 4`**: dorme `300ms` (fechamento de caixa), registra a **caixa completa** no `Relatorio` e limpa a `caixaAberta` para começar a próxima.

**O que o Yago vai explicar na apresentação:**
* O ciclo de vida das máquinas e por que o processamento (`sleep`) fica fora do `mutex` da fila.
* Por que `S1` e `P1` liberam o semáforo do exaustor **antes** de tentar inserir na próxima fila (para evitar que `S1` segure o exaustor esperando vaga na Pintura enquanto `P1` precisa do exaustor para esvaziar a Pintura — prevenção de *deadlock*).
* A regra determinística da inspeção (múltiplos de 7) e o fechamento de caixas completas e parciais.

---

### PARTE 3: Eduardo — Orquestração (`Fabrica`), Encerramento Coordenado e Testes
**Arquivos que o Eduardo vai criar:** `Fabrica.java` e `README.md`.

* **`Fabrica.java` (Classe Principal com método `main`)**:
  1. **Instanciação dos objetos compartilhados:**
     * Cria o `Relatorio`.
     * Cria as 6 filas (`filaCorte`, `filaFuracao`, `filaSoldagem`, `filaPintura`, `filaInspecao`, `filaEmbalagem`).
     * Cria **um único** `Semaphore exaustor = new Semaphore(1)` e passa a mesma instância para `S1` e `P1`.
  2. **Criação e Início das Threads:**
     * Cria as 8 threads (`C1, C2, F1, S1, P1, I1, I2, E1`).
     * Garantir que `C1` e `C2` recebam a mesma `filaCorte` e `filaFuracao`, e que `I1` e `I2` recebam a mesma `filaInspecao` e `filaEmbalagem`.
     * Dá `.start()` nas 8 threads **antes** de começar a gerar peças.
  3. **Geração das Peças (Produtor Inicial):**
     * Loop de `id = 1` até `Configuracao.QUANTIDADE_PECAS` (24).
     * Cria `new Peca(id)`, coloca na `filaCorte.colocar(peca)` e dorme `50ms` (`INTERVALO_CHEGADA`).
  4. **Protocolo Obrigatório de Encerramento Coordenado (Página 6 do PDF):**
     * Coloca 2 marcadores `FIM` (`new Peca(-1, true)`) na `filaCorte`.
     * Aguarda `tC1.join()` e `tC2.join()`.
     * Coloca 1 `FIM` na `filaFuracao` e aguarda `tF1.join()`.
     * Coloca 1 `FIM` na `filaSoldagem` e aguarda `tS1.join()`.
     * Coloca 1 `FIM` na `filaPintura` e aguarda `tP1.join()`.
     * Coloca 2 `FIM` na `filaInspecao` e aguarda `tI1.join()` e `tI2.join()`.
     * Coloca 1 `FIM` na `filaEmbalagem` e aguarda `tE1.join()`.
  5. **Exibição do Relatório e Configuração dos 5 Experimentos:**
     * Imprime no terminal o relatório completo exigido na Página 7/8 (criadas, aprovadas, reprovadas, embaladas, conteúdo de cada caixa, total por máquina, ocupação máxima de cada fila, verificação de filas vazias ao final e tempo total).
     * Permite escolher facilmente qual dos 5 experimentos rodar (por exemplo, passando argumento no terminal `java Fabrica 1` até `java Fabrica 5`, ou alterando uma constante simples).
* **`README.md`**:
  * Instruções claras de como compilar (`javac *.java`), executar (`java Fabrica`) e onde alterar os parâmetros para rodar os 5 experimentos obrigatórios.

**O que o Eduardo vai explicar na apresentação:**
* Como as filas e o exaustor são compartilhados por referência entre as threads no `main`.
* Como funciona o encerramento coordenado com `FIM` + `join()` etapa por etapa, e por que isso garante que nenhuma máquina encerre antes de processar todas as peças pendentes.
* Os resultados obtidos nos 5 experimentos obrigatórios.

---

## 3. Fluxo de Trabalho no Git e Uso dos Agentes (Passo a Passo)

1. **Documento-Guia Gerado (Este arquivo):**
   * Salve este arquivo como `PLANO_PROJETO.md` na pasta do projeto para servir de prompt/contexto para os agentes no Antigravity.

2. **Etapa do Fabrício (Integrante 1):**
   * Cria o repositório no GitHub.
   * Pede para o agente no Antigravity implementar a **Parte 1** (`Configuracao.java`, `Peca.java`, `FilaLimitada.java` e `Relatorio.java`) seguindo estritamente este documento.
   * Faz um teste rápido da fila, dá `git add .`, `git commit -m "Parte 1: Base, Peca e FilaLimitada com semaforos"` e `git push`.

3. **Etapa do Yago (Integrante 2):**
   * Dá `git clone` no repositório.
   * Pede para o agente no Antigravity ler os arquivos do Fabrício e implementar a **Parte 2** (`Maquina.java` e `MaquinaEmbalagem.java`) seguindo este documento.
   * Testa a compilação (`javac *.java`), dá `git commit -m "Parte 2: Threads das maquinas, exaustor, inspecao e embalagem"` e `git push`.

4. **Etapa do Eduardo (Integrante 3):**
   * Dá `git clone` (ou `git pull`) no repositório.
   * Pede para o agente no Antigravity ler todo o código existente e implementar a **Parte 3** (`Fabrica.java` com os 5 cenários de teste e `README.md`).
   * Executa os 5 testes obrigatórios para validar se os números batem exatamente com o gabarito da Página 7 do PDF (24 peças, 3 reprovadas [7, 14, 21], 21 aprovadas, 5 caixas cheias + 1 parcial com 1 peça, 141 visitas).
   * Dá `git commit -m "Parte 3: Orquestracao da Fabrica, encerramento coordenado e testes"` e `git push`.

5. **Etapa Final — Geração do Documento-Guia de Sustentação e Relatório Técnico:**
   * Após todas as implementações e commits estarem concluídos, **DEVE ser gerado um documento final de explicação e sustentação**, mas isso **não precisa ser feito aqui no chat web**.
   * Vocês devem utilizar os próprios agentes locais do **Antigravity** (que já têm acesso direto a todos os arquivos `.java` do repositório e uma janela de contexto maior) para ler o código pronto e gerar um arquivo `GUIA_APRESENTACAO_E_RELATORIO.md` contendo:
     1. A explicação resumida e intuitiva de cada classe apontando para o código real implementado.
     2. As respostas prontas para as **6 perguntas obrigatórias do relatório** (listadas na Página 10 do PDF do professor).
     3. Um roteiro rápido de defesa oral com possíveis perguntas que o professor pode fazer para o Fabrício, para o Yago e para o Eduardo durante a apresentação.