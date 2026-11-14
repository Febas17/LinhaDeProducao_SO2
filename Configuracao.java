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

    // Durações das operações (em ms)
    public static int DURACAO_CORTE = 120;
    public static int DURACAO_FURACAO = 100;
    public static int DURACAO_SOLDAGEM = 130;
    public static int DURACAO_PINTURA = 140;
    public static int DURACAO_INSPECAO = 110;
    public static int DURACAO_EMBALAGEM_PECA = 150;
    public static int DURACAO_FECHAMENTO_CAIXA = 300;

    // Calcula o tempo ajustado pelo fator de tempo
    public static long tempo(int ms) {
        return Math.max(1, Math.round(ms * FATOR_TEMPO));
    }
}
