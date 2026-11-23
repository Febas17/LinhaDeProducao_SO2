# Projeto 2 — Linha de Produção Concorrente (Sistemas Operacionais II)

**Professor:** Victor Lattari  
**Faculdade:** ESEG - Faculdade do Grupo Etapa  
**Integrantes:**
- Fabrício Saraiva (Parte 1: Estruturas Base, Peça, Buffer Concorrente e Log)
- Yago França (Parte 2: Threads das Máquinas, Exaustor, Inspeção e Embalagem)
- Eduardo Fadini (Parte 3: Orquestração `Fabrica`, Encerramento Coordenado em Cascata e Testes)

---

## 1. Visão Geral do Sistema

Este projeto implementa uma simulação concorrente em Java de uma linha de produção industrial para fabricação de suportes metálicos. Cada peça passa sequencialmente pelas seguintes etapas:

$$\text{Corte } (C1, C2) \longrightarrow \text{Furação } (F1) \longrightarrow \text{Soldagem } (S1) \longrightarrow \text{Pintura } (P1) \longrightarrow \text{Inspeção } (I1, I2) \longrightarrow \text{Embalagem } (E1)$$

### Destaques de Sistemas Operacionais
* **Filas Limitadas Reutilizáveis (`FilaLimitada.java`):** Cada setor intermediário possui uma fila de capacidade finita sincronizada por **três semáforos reais** (`Semaphore`):
  * `vagas`: Controla o espaço disponível (bloqueia produtor quando cheia).
  * `itens`: Controla a quantidade de peças disponíveis (bloqueia consumidor quando vazia).
  * `mutex`: Garante exclusão mútua na manipulação do `ArrayDeque` e na atualização dos contadores.
* **Recurso Auxiliar Compartilhado (Exaustor):** As máquinas de Soldagem ($S1$) e Pintura ($P1$) disputam uma única unidade de exaustão (`Semaphore exaustor = new Semaphore(1)`). A liberação do exaustor ocorre estritamente **antes** de tentar inserir a peça na fila seguinte, prevenindo deadlocks cíclicos.
* **Inspeção Determinística:** Peças com $\text{ID} \pmod 7 == 0$ são reprovadas e descartadas; as demais são aprovadas e encaminhadas para a embalagem.
* **Embalagem em Caixas:** A máquina $E1$ acomoda as peças e fecha caixas de 4 unidades. Ao receber o término da produção, fecha a última caixa mesmo que parcial (com 1 a 3 peças).
* **Encerramento Coordenado em Cascata:** O programa principal (`Fabrica.java`) envia marcadores sentinela `FIM` setor por setor, aguardando com `join()` que os produtores anteriores terminem antes de sinalizar a próxima etapa. Nenhuma peça é perdida e nenhuma thread fica órfã.

---

## 2. Como Compilar e Executar

O projeto foi construído em Java puro (sem pacotes externos), permitindo compilação e execução direta via terminal:

### Compilação:
```bash
javac *.java
```

### Execução:

#### 1. Modo Terminal (Texto / Logs Oficiais de SO):
Por padrão, executar sem argumentos roda o **Experimento 1 (Fluxo Completo)**:
```bash
java Fabrica
```

Para rodar qualquer um dos **5 experimentos obrigatórios**, passe o número do cenário como parâmetro (1 a 5):
```bash
java Fabrica 1   # Experimento 1: Fluxo Completo (Caso Inicial)
java Fabrica 2   # Experimento 2: Armazenamento Reduzido (Filas com Capacidade 1)
java Fabrica 3   # Experimento 3: Inspeção Lenta (Gargalo e Propagação de Espera)
java Fabrica 4   # Experimento 4: Disputa por Exaustor (Soldagem e Pintura com 600ms)
java Fabrica 5   # Experimento 5: Carga Ampliada (48 peças, sem pausa, fator 0.1)
```

#### 2. Modo Gráfico Pixel Art (Interface Visual da Linha de Produção):
Para abrir a interface gráfica animada em tempo real com esteira, iluminação de máquinas, duto do exaustor e caixas:
```bash
java FabricaGUI
```

---

## 3. Os 5 Experimentos Obrigatórios

| Cenário | Descrição | Principais Configurações | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **1** | **Fluxo Completo (Caso Inicial)** | 24 peças, chegada 50ms, capacidades padrão (5, 3, 3, 3, 4, 4), tempos nominais | 24 criadas, 21 aprovadas, 3 descartadas (7, 14, 21), 6 caixas (5 cheias + 1 com 1 peça), 141 visitas |
| **2** | **Armazenamento Reduzido** | Todas as filas limitadas com capacidade **1** | Resultados finais idênticos ao Caso 1; ocupação máxima de todas as filas $\le 1$ |
| **3** | **Inspeção Lenta** | $I1 = 2500\text{ ms}$, $I2 = 3000\text{ ms}$ | Fila de inspeção satura e a espera se propaga para Pintura, Soldagem e Furação |
| **4** | **Disputa por Exaustão** | $S1 = 600\text{ ms}$, $P1 = 600\text{ ms}$ | Concorrência acentuada pelo exaustor com exclusão mútua estrita |
| **5** | **Carga Ampliada** | 48 peças, chegada sem pausa ($0\text{ ms}$), fator de tempo $0.1$ | 48 criadas, 42 embaladas, 6 descartadas, 11 caixas (10 cheias + 1 com 2 peças) |

> **Nota sobre Contagem de Ocupação:** A maior ocupação registrada nas filas considera a ocupação física real de elementos simultâneos dentro do buffer, podendo incluir momentaneamente os marcadores sentinela `FIM` durante o protocolo de encerramento em cascata.

---

## 4. Onde Alterar os Parâmetros

Todos os parâmetros da fábrica estão centralizados na classe `Configuracao.java`:
- `QUANTIDADE_PECAS`: Total de peças produzidas na simulação (padrão: 24).
- `INTERVALO_CHEGADA_MS`: Intervalo de injeção de peças no corte (padrão: 50 ms).
- `FATOR_TEMPO`: Multiplicador de escala de tempo para aceleração de testes (ex: 1.0 = tempo real; 0.1 = 10x mais rápido).
- `CAPACIDADE_...`: Capacidade das filas de cada setor.
- `DURACAO_...`: Tempos de processamento individuais de cada máquina e da embalagem.

---

## 5. Estrutura de Arquivos

* `Configuracao.java`: Central de constantes, tempos e seletor dos 5 cenários.
* `Peca.java`: Estrutura de dados com ID, histórico de passagem e status de inspeção.
* `FilaLimitada.java`: Buffer limitado concorrente implementado com `ArrayDeque` e 3 `Semaphore`s (`vagas`, `itens`, `mutex`).
* `Relatorio.java`: Monitor thread-safe de eventos e gerador do balanço final consolidado.
* `Maquina.java`: Thread trabalhadora padrão (`C1, C2, F1, S1, P1, I1, I2`).
* `MaquinaEmbalagem.java`: Thread da embaladora `E1` com lógica de fechamento de caixas completas e parciais.
* `Fabrica.java`: Orquestrador principal (instanciação, injeção de peças, encerramento em cascata e verificação).
* `FabricaGUI.java`: Interface gráfica interativa estilo Pixel Art 16-bit com esteira animada, monitoramento das máquinas, ocupação dos buffers, duto do exaustor e caixas.
