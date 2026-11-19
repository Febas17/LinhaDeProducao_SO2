# Guia Completo de Apresentação, Sustentação Oral e Relatório Técnico

**Disciplina:** Sistemas Operacionais II  
**Professor:** Victor Lattari  
**Integrantes:** Fabrício Saraiva, Yago França e Eduardo Fadini  

---

## 1. O que o Eduardo (Integrante 3) vai defender na Sustentação Oral

Como responsável pela **orquestração da fábrica, encerramento coordenado e execução dos experimentos**, suas falas principais devem focar em como todo o sistema foi conectado e como o ciclo de vida das threads foi finalizado sem deadlocks e sem perda de dados:

### 1.1. Compartilhamento por Referência de Filas e Semáforos
* **O que explicar:**  
  "No método `main` da classe `Fabrica`, criamos uma única instância de cada fila limitada e um único semáforo de exaustor (`Semaphore exaustor = new Semaphore(1)`). Essas instâncias exatas foram passadas por parâmetro para os construtores das máquinas. Por exemplo, tanto $C1$ quanto $C2$ recebem a mesma referência para `filaCorte` e `filaFuracao`, competindo concorrentemente pelas peças. Da mesma forma, $S1$ e $P1$ recebem o **mesmo objeto** `exaustor`, garantindo que apenas uma delas utilize o recurso por vez."

### 1.2. O Protocolo Obrigatório de Encerramento em Cascata (`FIM` + `join()`)
* **O que explicar:**  
  "Não basta verificar se uma fila está vazia para encerrar uma máquina, porque uma máquina anterior pode estar processando uma peça que ainda será colocada nessa fila. Para resolver isso com segurança:
  1. Primeiro, geramos todas as 24 peças.
  2. Em seguida, colocamos **dois marcadores `FIM`** na `filaCorte` (um para $C1$ e um para $C2$) e usamos `tC1.join()` e `tC2.join()`.
  3. O `join()` bloqueia a thread coordenadora até que $C1$ e $C2$ retirem seus marcadores e terminem completamente. Quando isso acontece, temos a certeza absoluta de que o setor de corte encerrou e que **todas as peças do corte já estão na fila de Furação**.
  4. Repetimos esse processo em cascata setor por setor: inserimos o `FIM` na Furação e aguardamos $F1$ com `join()`; depois na Soldagem ($S1$), Pintura ($P1$), Inspeção ($I1$ e $I2$) e por fim na Embalagem ($E1$).
  5. Isso garante que nenhuma máquina encerre prematuramente, nenhum dado seja perdido e que todas as filas terminem rigorosamente vazias."

### 1.3. Análise dos 5 Experimentos Obrigatórios
* **O que explicar:**
  * **Experimento 1 (Fluxo Completo):** Validamos com 24 peças o gabarito exato do professor: 24 criadas, 3 reprovadas ($IDs\ 7, 14, 21$), 21 aprovadas e embaladas em 5 caixas cheias e 1 caixa parcial com 1 peça, totalizando exatamente 141 visitas às máquinas.
  * **Experimento 2 (Armazenamento Reduzido = 1):** Com capacidade 1 em todas as filas, o throughput reduz um pouco devido ao bloqueio frequente nas saídas (`AGUARDANDO SAÍDA`), mas o sistema mantém a consistência matemática perfeita e nenhuma fila excede tamanho 1.
  * **Experimento 3 (Inspeção Lenta):** Ao elevar os tempos de inspeção ($2500\text{ ms}$ e $3000\text{ ms}$), a fila de inspeção rapidamente satura, provocando um efeito dominó de espera retroativa (backpressure) que retém a Pintura e a Soldagem.
  * **Experimento 4 (Disputa por Exaustor):** Com Soldagem e Pintura em $600\text{ ms}$, observamos alternância estrita no uso do exaustor, comprovando que as operações nunca ocorrem simultaneamente.
  * **Experimento 5 (Carga Ampliada):** Com 48 peças e fator acelerado de $0.1$, tivemos 6 descartes, 42 embaladas, 10 caixas completas e 1 parcial com 2 peças, provando a escalabilidade do modelo.

---

## 2. Respostas Prontas para as 6 Perguntas Obrigatórias do Relatório (Página 10 do PDF)

### Pergunta 1: Por que o mutex da fila não substitui os semáforos de itens e vagas?
**Resposta:**  
O `mutex` (semáforo binário com 1 permissão) tem como único propósito garantir a **exclusão mútua**, ou seja, assegurar que duas threads não acessem ou modifiquem simultaneamente a estrutura interna da fila (`ArrayDeque`) e seus contadores (`ocupacaoAtual`). No entanto, o `mutex` não possui noção de limite de capacidade ou de disponibilidade: ele não bloqueia uma thread produtora quando a fila está cheia, nem uma consumidora quando está vazia. Se usássemos apenas o mutex, teríamos que fazer *busy-waiting* (espera ativa com loops e sleep), o que desperdiça ciclos de CPU e viola as boas práticas de SO. Os semáforos contadores `vagas` e `itens` realizam a **sincronização de condição**: bloqueiam a thread fora do mutex até que haja espaço para produzir ou item para consumir.

---

### Pergunta 2: O que acontece quando a pintura não consegue encaminhar uma peça?
**Resposta:**  
Quando a fila de inspeção está cheia (`vagas == 0`), a máquina de Pintura ($P1$) tenta inserir a peça chamando `filaSaida.colocar(peca)`. O semáforo `vagas.acquire()` bloqueia a thread de Pintura. Ela permanece no estado `AGUARDANDO SAÍDA`, retendo consigo a peça recém-pintada e impedida de iniciar o processamento de uma nova peça da fila anterior, exercendo um controle de fluxo natural (*backpressure*).

---

### Pergunta 3: Por que liberar o exaustor antes dessa espera?
**Resposta:**  
Para **prevenir deadlocks (impasses)**. O exaustor é um recurso compartilhado entre Soldagem ($S1$) e Pintura ($P1$). Se $S1$ finalizasse sua soldagem mas mantivesse o exaustor adquirido enquanto tenta inserir a peça na fila de Pintura (que pode estar cheia), e $P1$ precisasse do exaustor para processar a peça da fila de Pintura e assim liberar uma vaga, formar-se-ia um **ciclo de espera circular**:
* $S1$ segura o exaustor e espera vaga na fila de Pintura.
* $P1$ segura a vaga na fila de Pintura e espera o exaustor detido por $S1$.  
Liberando o exaustor no bloco `finally` logo após o `Thread.sleep` da operação e **antes** de chamar `filaSaida.colocar(peca)`, quebramos a condição de "posse e espera" (*hold and wait*), impossibilitando a formação de deadlock entre os setores.

---

### Pergunta 4: Por que duas máquinas do mesmo setor precisam compartilhar a fila?
**Resposta:**  
Para viabilizar o balanceamento de carga e o paralelismo real de tarefas homogêneas sem replicação desnecessária de buffers. Se cada máquina de corte ($C1$ e $C2$) tivesse sua própria fila independente, um produtor teria que escolher previamente em qual fila colocar a peça, o que poderia gerar situações onde uma máquina fica ociosa enquanto a outra acumula fila (desbalanceamento). Com uma fila única compartilhada protegida por semáforos, a primeira máquina que ficar livre retira atomicamente a próxima peça disponível, garantindo máxima utilização dos recursos de processamento.

---

### Pergunta 5: Como o protocolo de FIM evita perda de peças?
**Resposta:**  
O marcador `FIM` é inserido na fila como um objeto comum (`Peca` com `fim = true` e `id = -1`). Como a fila segue a política FIFO (*First-In, First-Out*), o marcador `FIM` só é retirado por uma máquina consumidora **depois** que todas as peças reais inseridas anteriormente foram processadas.  
Além disso, o coordenador (`Fabrica`) utiliza um encerramento coordenado em cascata: ele aguarda com `join()` a terminação completa de todos os trabalhadores do setor anterior antes de injetar o `FIM` no setor seguinte. Isso impede que setores a jusante finalizem suas atividades enquanto ainda existem peças em trânsito a montante.

---

### Pergunta 6: Como os registros demonstram concorrência e integridade?
**Resposta:**  
* **Concorrência:** Nos logs de terminal com carimbo de tempo em milissegundos (`[ms]`), observamos intercalação temporal contínua entre as máquinas (ex: $C1$ iniciando corte enquanto $F1$ fura e $S1$ solda). No setor de corte e inspeção, os totais processados dividem-se dinamicamente entre $C1/C2$ e $I1/I2$ dependendo da duração de cada uma.
* **Integridade:**
  1. A soma das peças processadas em cada setor ($C1 + C2$, $F1$, $S1$, $P1$, $I1 + I2$) é exatamente 24.
  2. Todas as 3 peças reprovadas pertencem estritamente ao conjunto de múltiplos de 7 ($\{7, 14, 21\}$).
  3. Todas as 21 peças aprovadas são embaladas em caixas sem duplicação e sem omissão de IDs.
  4. Ao final, a verificação comprova que todas as filas terminaram com ocupação zero e nenhuma peça ficou sem destino final.

---

## 3. Resumo Rápido de Cada Classe para Consulta Durante a Apresentação

* **`Configuracao.java`:** Centraliza parâmetros globais (quantidades, tempos nominais em ms, capacidades das filas e o método `configurarCenario(1..5)`).
* **`Peca.java`:** Objeto de dados simples contendo ID, flag de encerramento (`fim`), histórico de máquinas percorridas e número da caixa.
* **`FilaLimitada.java`:** Buffer limitado sincronizado exclusivamente com 3 semáforos reais (`vagas`, `itens`, `mutex`) sobre um `ArrayDeque`.
* **`Relatorio.java`:** Registrador thread-safe (com `mutexRelatorio`) para emissão de logs sem linhas corrompidas e consolidação do relatório de auditoria final.
* **`Maquina.java`:** Thread padrão das etapas industriais ($C1, C2, F1, S1, P1, I1, I2$), com controle do exaustor e filtro de inspeção.
* **`MaquinaEmbalagem.java`:** Thread da embaladora $E1$, gerencia a caixa aberta de 4 unidades e o fechamento da caixa parcial na chegada do marcador `FIM`.
* **`Fabrica.java`:** Ponto de entrada (`main`), orquestra a criação dos semáforos/filas, dispara as threads, injeta a carga inicial e executa o encerramento em cascata com `join()`.
