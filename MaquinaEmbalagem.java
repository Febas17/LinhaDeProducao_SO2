import java.util.ArrayList;
import java.util.List;

public class MaquinaEmbalagem implements Runnable {
    private static final int CAPACIDADE_CAIXA = 4;

    private final String nome;
    private final FilaLimitada filaEmbalagem;
    private final int duracaoEmbalagemPecaMs;
    private final int duracaoFechamentoCaixaMs;
    private final Relatorio relatorio;
    private final List<Integer> caixaAberta;
    private final List<Peca> pecasCaixaAberta;

    public MaquinaEmbalagem(
            String nome,
            FilaLimitada filaEmbalagem,
            int duracaoEmbalagemPecaMs,
            int duracaoFechamentoCaixaMs,
            Relatorio relatorio) {
        this.nome = nome;
        this.filaEmbalagem = filaEmbalagem;
        this.duracaoEmbalagemPecaMs = duracaoEmbalagemPecaMs;
        this.duracaoFechamentoCaixaMs = duracaoFechamentoCaixaMs;
        this.relatorio = relatorio;
        this.caixaAberta = new ArrayList<>();
        this.pecasCaixaAberta = new ArrayList<>();
    }

    public MaquinaEmbalagem(String nome, FilaLimitada filaEmbalagem, Relatorio relatorio) {
        this(
                nome,
                filaEmbalagem,
                Configuracao.DURACAO_EMBALAGEM_PECA,
                Configuracao.DURACAO_FECHAMENTO_CAIXA,
                relatorio);
    }

    public MaquinaEmbalagem(FilaLimitada filaEmbalagem, Relatorio relatorio) {
        this(
                "E1",
                filaEmbalagem,
                Configuracao.DURACAO_EMBALAGEM_PECA,
                Configuracao.DURACAO_FECHAMENTO_CAIXA,
                relatorio);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Peca peca = filaEmbalagem.retirar();

                if (peca.isFim()) {
                    if (!caixaAberta.isEmpty()) {
                        fecharCaixa(peca.id, "PARCIAL");
                    }
                    relatorio.log(nome, peca.id, "RECEBEU FIM - ENCERRANDO");
                    break;
                }

                relatorio.log(nome, peca.id, "INICIO OPERACAO");
                Thread.sleep(Configuracao.tempo(duracaoEmbalagemPecaMs));
                peca.adicionarEtapa("Embalagem:" + nome);
                caixaAberta.add(peca.id);
                pecasCaixaAberta.add(peca);
                relatorio.registrarPecaNaMaquina(nome);
                relatorio.log(nome, peca.id, "FIM OPERACAO (" + caixaAberta.size() + "/" + CAPACIDADE_CAIXA + " NA CAIXA)");

                if (caixaAberta.size() == CAPACIDADE_CAIXA) {
                    fecharCaixa(peca.id, "COMPLETA");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void fecharCaixa(int pecaIdLog, String tipoCaixa) throws InterruptedException {
        relatorio.log(nome, pecaIdLog, "FECHANDO CAIXA " + tipoCaixa);
        Thread.sleep(Configuracao.tempo(duracaoFechamentoCaixaMs));
        int numeroCaixa = relatorio.registrarCaixa(caixaAberta);
        for (Peca p : pecasCaixaAberta) {
            p.numeroCaixa = numeroCaixa;
        }
        relatorio.log(nome, pecaIdLog, "CAIXA " + tipoCaixa + " " + numeroCaixa + " FECHADA: " + caixaAberta);
        caixaAberta.clear();
        pecasCaixaAberta.clear();
    }
}

