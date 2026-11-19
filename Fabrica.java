import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Fabrica {

    public static void main(String[] args) {
        int cenario = 1;

        if (args.length > 0) {
            try {
                cenario = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Argumento invalido. Uso: java Fabrica [1|2|3|4|5]");
                System.out.println("Executando o Cenario 1 por padrao...\n");
                cenario = 1;
            }
        }

        executarCenario(cenario);
    }

    public static void executarCenario(int cenario) {
        // 1. Configurar parâmetros do cenário escolhido
        Configuracao.configurarCenario(cenario);

        System.out.println("=================================================");
        System.out.println("   INICIANDO SIMULACAO DA LINHA DE PRODUCAO");
        System.out.println("   Cenario selecionado: " + cenario);
        System.out.println("   Quantidade de pecas: " + Configuracao.QUANTIDADE_PECAS);
        System.out.println("   Fator de tempo: " + Configuracao.FATOR_TEMPO);
        System.out.println("=================================================\n");

        Relatorio relatorio = new Relatorio();

        // 2. Instanciação das 6 filas limitadas com Semáforos
        FilaLimitada filaCorte = new FilaLimitada("Fila-Corte", Configuracao.CAPACIDADE_CORTE);
        FilaLimitada filaFuracao = new FilaLimitada("Fila-Furacao", Configuracao.CAPACIDADE_FURACAO);
        FilaLimitada filaSoldagem = new FilaLimitada("Fila-Soldagem", Configuracao.CAPACIDADE_SOLDAGEM);
        FilaLimitada filaPintura = new FilaLimitada("Fila-Pintura", Configuracao.CAPACIDADE_PINTURA);
        FilaLimitada filaInspecao = new FilaLimitada("Fila-Inspecao", Configuracao.CAPACIDADE_INSPECAO);
        FilaLimitada filaEmbalagem = new FilaLimitada("Fila-Embalagem", Configuracao.CAPACIDADE_EMBALAGEM);

        // 3. Instanciação do recurso auxiliar compartilhado (Exaustor)
        Semaphore exaustor = new Semaphore(1);

        // 4. Instanciação das 8 máquinas concorrentes
        // Setor de Corte (compartilham filaCorte e filaFuracao)
        Maquina c1 = new Maquina("C1", "Corte", filaCorte, filaFuracao, Configuracao.DURACAO_CORTE_C1, relatorio);
        Maquina c2 = new Maquina("C2", "Corte", filaCorte, filaFuracao, Configuracao.DURACAO_CORTE_C2, relatorio);

        // Setor de Furação
        Maquina f1 = new Maquina("F1", "Furacao", filaFuracao, filaSoldagem, Configuracao.DURACAO_FURACAO, relatorio);

        // Setor de Soldagem (compartilha exaustor)
        Maquina s1 = new Maquina("S1", "Soldagem", filaSoldagem, filaPintura, Configuracao.DURACAO_SOLDAGEM, exaustor, relatorio);

        // Setor de Pintura (compartilha exaustor)
        Maquina p1 = new Maquina("P1", "Pintura", filaPintura, filaInspecao, Configuracao.DURACAO_PINTURA, exaustor, relatorio);

        // Setor de Inspeção (compartilham filaInspecao e filaEmbalagem)
        Maquina i1 = new Maquina("I1", "Inspecao", filaInspecao, filaEmbalagem, Configuracao.DURACAO_INSPECAO_I1, relatorio);
        Maquina i2 = new Maquina("I2", "Inspecao", filaInspecao, filaEmbalagem, Configuracao.DURACAO_INSPECAO_I2, relatorio);

        // Setor de Embalagem
        MaquinaEmbalagem e1 = new MaquinaEmbalagem(
                "E1",
                filaEmbalagem,
                Configuracao.DURACAO_EMBALAGEM_PECA,
                Configuracao.DURACAO_FECHAMENTO_CAIXA,
                relatorio
        );

        // 5. Criação e inicialização de todas as threads consumidoras antes de preencher as filas
        Thread tC1 = new Thread(c1, "Thread-C1");
        Thread tC2 = new Thread(c2, "Thread-C2");
        Thread tF1 = new Thread(f1, "Thread-F1");
        Thread tS1 = new Thread(s1, "Thread-S1");
        Thread tP1 = new Thread(p1, "Thread-P1");
        Thread tI1 = new Thread(i1, "Thread-I1");
        Thread tI2 = new Thread(i2, "Thread-I2");
        Thread tE1 = new Thread(e1, "Thread-E1");

        tC1.start();
        tC2.start();
        tF1.start();
        tS1.start();
        tP1.start();
        tI1.start();
        tI2.start();
        tE1.start();

        try {
            // 6. Geração das peças (Produtor Inicial)
            for (int id = 1; id <= Configuracao.QUANTIDADE_PECAS; id++) {
                Peca peca = new Peca(id);
                relatorio.log("Fabrica", id, "PECA GERADA E ENVIADA AO CORTE");
                filaCorte.colocar(peca);

                if (Configuracao.INTERVALO_CHEGADA_MS > 0) {
                    Thread.sleep(Configuracao.tempo(Configuracao.INTERVALO_CHEGADA_MS));
                }
            }

            // 7. Protocolo Obrigatório de Encerramento Coordenado em Cascata (Seção 5 do PDF)
            // Etapa 1: Envia dois marcadores FIM para a Fila de Corte e aguarda C1 e C2
            filaCorte.colocar(new Peca(-1, true));
            filaCorte.colocar(new Peca(-1, true));
            tC1.join();
            tC2.join();

            // Etapa 2: Só então envia um FIM para Furação e aguarda F1
            filaFuracao.colocar(new Peca(-1, true));
            tF1.join();

            // Etapa 3: Envia um FIM para Soldagem e aguarda S1
            filaSoldagem.colocar(new Peca(-1, true));
            tS1.join();

            // Etapa 4: Envia um FIM para Pintura e aguarda P1
            filaPintura.colocar(new Peca(-1, true));
            tP1.join();

            // Etapa 5: Envia dois marcadores FIM para Inspeção e aguarda I1 e I2
            filaInspecao.colocar(new Peca(-1, true));
            filaInspecao.colocar(new Peca(-1, true));
            tI1.join();
            tI2.join();

            // Etapa 6: Envia um FIM para Embalagem e aguarda E1
            filaEmbalagem.colocar(new Peca(-1, true));
            tE1.join();

            // 8. Exibição do relatório final automático com validação de dados
            List<FilaLimitada> todasAsFilas = Arrays.asList(
                    filaCorte,
                    filaFuracao,
                    filaSoldagem,
                    filaPintura,
                    filaInspecao,
                    filaEmbalagem
            );

            relatorio.imprimirResumoFinal(todasAsFilas, Configuracao.QUANTIDADE_PECAS);

        } catch (InterruptedException e) {
            System.err.println("Execucao interrompida: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
