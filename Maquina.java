import java.util.concurrent.Semaphore;

public class Maquina implements Runnable {
    private final String nome;
    private final String setor;
    private final FilaLimitada filaEntrada;
    private final FilaLimitada filaSaida;
    private final int duracaoMs;
    private final Semaphore exaustor;
    private final Relatorio relatorio;

    public Maquina(
            String nome,
            String setor,
            FilaLimitada filaEntrada,
            FilaLimitada filaSaida,
            int duracaoMs,
            Semaphore exaustor,
            Relatorio relatorio) {
        this.nome = nome;
        this.setor = setor;
        this.filaEntrada = filaEntrada;
        this.filaSaida = filaSaida;
        this.duracaoMs = duracaoMs;
        this.exaustor = exaustor;
        this.relatorio = relatorio;
    }

    public Maquina(
            String nome,
            String setor,
            FilaLimitada filaEntrada,
            FilaLimitada filaSaida,
            int duracaoMs,
            Relatorio relatorio) {
        this(nome, setor, filaEntrada, filaSaida, duracaoMs, null, relatorio);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Peca peca = filaEntrada.retirar();

                if (peca.isFim()) {
                    relatorio.log(nome, peca.id, "RECEBEU FIM - ENCERRANDO");
                    break;
                }

                if (exaustor != null) {
                    exaustor.acquire();
                    relatorio.log(nome, peca.id, "ADQUIRIU EXAUSTOR");
                }

                try {
                    relatorio.log(nome, peca.id, "INICIO OPERACAO");
                    Thread.sleep(Configuracao.tempo(duracaoMs));
                    peca.adicionarEtapa(setor + ":" + nome);
                    relatorio.registrarPecaNaMaquina(nome);
                    relatorio.log(nome, peca.id, "FIM OPERACAO");
                } finally {
                    if (exaustor != null) {
                        exaustor.release();
                        relatorio.log(nome, peca.id, "LIBEROU EXAUSTOR");
                    }
                }

                if (ehInspecao()) {
                    if (peca.id % 7 == 0) {
                        peca.aprovada = false;
                        relatorio.registrarDescartada(peca.id);
                        relatorio.log(nome, peca.id, "REPROVADA NA INSPECAO (DESCARTADA)");
                    } else {
                        peca.aprovada = true;
                        relatorio.registrarAprovada(peca.id);
                        relatorio.log(nome, peca.id, "APROVADA NA INSPECAO - AGUARDANDO SAIDA");
                        filaSaida.colocar(peca);
                    }
                } else {
                    relatorio.log(nome, peca.id, "AGUARDANDO SAIDA");
                    filaSaida.colocar(peca);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean ehInspecao() {
        return (setor != null && (setor.equalsIgnoreCase("Inspecao") || setor.equalsIgnoreCase("Inspeção")))
                || (nome != null && nome.startsWith("I"));
    }
}

