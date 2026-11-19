public class Configuracao {
    // Parâmetros gerais da simulação
    public static int QUANTIDADE_PECAS = 24;
    public static int INTERVALO_CHEGADA_MS = 50;
    public static double FATOR_TEMPO = 1.0;

    // Capacidades das filas
    public static int CAPACIDADE_CORTE = 5;
    public static int CAPACIDADE_FURACAO = 3;
    public static int CAPACIDADE_SOLDAGEM = 3;
    public static int CAPACIDADE_PINTURA = 3;
    public static int CAPACIDADE_INSPECAO = 4;
    public static int CAPACIDADE_EMBALAGEM = 4;

    // Durações das operações nominais (em ms) conforme Tabela da Seção 2 do PDF
    public static int DURACAO_CORTE_C1 = 200;
    public static int DURACAO_CORTE_C2 = 300;
    public static int DURACAO_CORTE = 200; // Compatibilidade

    public static int DURACAO_FURACAO = 250;
    public static int DURACAO_SOLDAGEM = 400;
    public static int DURACAO_PINTURA = 500;

    public static int DURACAO_INSPECAO_I1 = 250;
    public static int DURACAO_INSPECAO_I2 = 350;
    public static int DURACAO_INSPECAO = 250; // Compatibilidade

    public static int DURACAO_EMBALAGEM_PECA = 150;
    public static int DURACAO_FECHAMENTO_CAIXA = 300;

    // Calcula o tempo ajustado pelo fator de tempo
    public static long tempo(int ms) {
        return Math.max(1, Math.round(ms * FATOR_TEMPO));
    }

    // Configura os parâmetros para cada um dos 5 experimentos obrigatórios (Seção 6 do PDF)
    public static void configurarCenario(int cenario) {
        // Restaura valores padrão nominais
        QUANTIDADE_PECAS = 24;
        INTERVALO_CHEGADA_MS = 50;
        FATOR_TEMPO = 1.0;

        CAPACIDADE_CORTE = 5;
        CAPACIDADE_FURACAO = 3;
        CAPACIDADE_SOLDAGEM = 3;
        CAPACIDADE_PINTURA = 3;
        CAPACIDADE_INSPECAO = 4;
        CAPACIDADE_EMBALAGEM = 4;

        DURACAO_CORTE_C1 = 200;
        DURACAO_CORTE_C2 = 300;
        DURACAO_FURACAO = 250;
        DURACAO_SOLDAGEM = 400;
        DURACAO_PINTURA = 500;
        DURACAO_INSPECAO_I1 = 250;
        DURACAO_INSPECAO_I2 = 350;
        DURACAO_EMBALAGEM_PECA = 150;
        DURACAO_FECHAMENTO_CAIXA = 300;

        switch (cenario) {
            case 1:
                // Experimento 1: Fluxo completo (caso inicial nominal)
                System.out.println("===> Executando Experimento 1: Fluxo Completo (Caso Inicial)");
                break;
            case 2:
                // Experimento 2: Armazenamento reduzido (todas as filas com capacidade 1)
                System.out.println("===> Executando Experimento 2: Armazenamento Reduzido (Todas as filas = 1)");
                CAPACIDADE_CORTE = 1;
                CAPACIDADE_FURACAO = 1;
                CAPACIDADE_SOLDAGEM = 1;
                CAPACIDADE_PINTURA = 1;
                CAPACIDADE_INSPECAO = 1;
                CAPACIDADE_EMBALAGEM = 1;
                break;
            case 3:
                // Experimento 3: Inspeção lenta (I1=2500ms, I2=3000ms)
                System.out.println("===> Executando Experimento 3: Inspecao Lenta (I1=2500ms, I2=3000ms)");
                DURACAO_INSPECAO_I1 = 2500;
                DURACAO_INSPECAO_I2 = 3000;
                break;
            case 4:
                // Experimento 4: Disputa por exaustor (Soldagem=600ms, Pintura=600ms)
                System.out.println("===> Executando Experimento 4: Disputa por Exaustor (Soldagem=600ms, Pintura=600ms)");
                DURACAO_SOLDAGEM = 600;
                DURACAO_PINTURA = 600;
                break;
            case 5:
                // Experimento 5: Carga ampliada (48 peças, chegada sem pausa, fator tempo 0.1)
                System.out.println("===> Executando Experimento 5: Carga Ampliada (48 pecas, sem pausa, fator 0.1)");
                QUANTIDADE_PECAS = 48;
                INTERVALO_CHEGADA_MS = 0;
                FATOR_TEMPO = 0.1;
                break;
            default:
                System.out.println("===> Cenario desconhecido (" + cenario + "). Usando configuracao padrao.");
                break;
        }
    }
}
