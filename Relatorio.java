import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class Relatorio {
    private final long inicioMs;
    private final Semaphore mutexRelatorio;

    private final Map<String, Integer> pecasPorMaquina;
    private final List<Integer> aprovadas;
    private final List<Integer> descartadas;
    private final List<List<Integer>> caixas;

    public Relatorio() {
        this.inicioMs = System.currentTimeMillis();
        this.mutexRelatorio = new Semaphore(1);
        this.pecasPorMaquina = new LinkedHashMap<>();
        this.aprovadas = new ArrayList<>();
        this.descartadas = new ArrayList<>();
        this.caixas = new ArrayList<>();
    }

    public void log(String maquina, int pecaId, String evento) {
        long tempo = System.currentTimeMillis() - inicioMs;
        String textoPeca = (pecaId == -1) ? "FIM" : String.valueOf(pecaId);

        try {
            mutexRelatorio.acquire();
            try {
                System.out.println("[" + tempo + " ms] [" + maquina + "] [Peca " + textoPeca + "] " + evento);
            } finally {
                mutexRelatorio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void registrarPecaNaMaquina(String maquina) {
        try {
            mutexRelatorio.acquire();
            try {
                int atual = pecasPorMaquina.getOrDefault(maquina, 0);
                pecasPorMaquina.put(maquina, atual + 1);
            } finally {
                mutexRelatorio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void registrarAprovada(int id) {
        try {
            mutexRelatorio.acquire();
            try {
                aprovadas.add(id);
            } finally {
                mutexRelatorio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void registrarDescartada(int id) {
        try {
            mutexRelatorio.acquire();
            try {
                descartadas.add(id);
            } finally {
                mutexRelatorio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int registrarCaixa(List<Integer> pecasCaixa) {
        try {
            mutexRelatorio.acquire();
            try {
                caixas.add(new ArrayList<>(pecasCaixa));
                return caixas.size();
            } finally {
                mutexRelatorio.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    public void imprimirResumoFinal(List<FilaLimitada> filas, int totalCriadas) {
        long tempoTotal = System.currentTimeMillis() - inicioMs;

        System.out.println("\n================ RELATORIO FINAL ================");
        System.out.println("Pecas criadas: " + totalCriadas);
        System.out.println("Pecas aprovadas (" + aprovadas.size() + "): " + aprovadas);
        System.out.println("Pecas descartadas (" + descartadas.size() + "): " + descartadas);

        int totalEmbaladas = 0;
        for (List<Integer> c : caixas) {
            totalEmbaladas += c.size();
        }
        System.out.println("Pecas embaladas: " + totalEmbaladas);
        System.out.println("Caixas fechadas (" + caixas.size() + "):");
        for (int i = 0; i < caixas.size(); i++) {
            System.out.println("  Caixa " + (i + 1) + ": " + caixas.get(i));
        }

        System.out.println("Total processado por maquina:");
        int totalVisitas = 0;
        for (Map.Entry<String, Integer> entry : pecasPorMaquina.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            totalVisitas += entry.getValue();
        }
        System.out.println("Total de visitas a etapas: " + totalVisitas);

        System.out.println("Ocupacao maxima das filas (pode incluir marcadores FIM):");
        boolean todasVazias = true;
        for (FilaLimitada f : filas) {
            System.out.println("  " + f.getNome() + ": max=" + f.getMaiorOcupacao() + " | vazia=" + f.estaVazia());
            if (!f.estaVazia()) {
                todasVazias = false;
            }
        }

        System.out.println("Todas as filas vazias ao final? " + (todasVazias ? "SIM" : "NAO"));
        boolean pecaSemDestino = (totalEmbaladas + descartadas.size()) != totalCriadas;
        System.out.println("Alguma peca sem destino final? " + (pecaSemDestino ? "SIM (ERRO)" : "NAO (Todas embaladas ou descartadas)"));
        System.out.println("Tempo total de simulacao: " + tempoTotal + " ms");
        System.out.println("=================================================");
    }
}
