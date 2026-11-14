import java.util.ArrayDeque;
import java.util.concurrent.Semaphore;

public class FilaLimitada {
    private final String nome;
    private final ArrayDeque<Peca> fila;
    private final Semaphore vagas;
    private final Semaphore itens;
    private final Semaphore mutex;

    private int ocupacaoAtual;
    private int maiorOcupacao;

    public FilaLimitada(String nome, int capacidade) {
        this.nome = nome;
        this.fila = new ArrayDeque<>();
        this.vagas = new Semaphore(capacidade);
        this.itens = new Semaphore(0);
        this.mutex = new Semaphore(1);
        this.ocupacaoAtual = 0;
        this.maiorOcupacao = 0;
    }

    public void colocar(Peca p) throws InterruptedException {
        vagas.acquire();
        mutex.acquire();
        try {
            fila.addLast(p);
            ocupacaoAtual++;
            if (ocupacaoAtual > maiorOcupacao) {
                maiorOcupacao = ocupacaoAtual;
            }
        } finally {
            mutex.release();
        }
        itens.release();
    }

    public Peca retirar() throws InterruptedException {
        itens.acquire();
        mutex.acquire();
        Peca p;
        try {
            p = fila.removeFirst();
            ocupacaoAtual--;
        } finally {
            mutex.release();
        }
        vagas.release();
        return p;
    }

    public String getNome() {
        return nome;
    }

    public int getOcupacaoAtual() {
        return ocupacaoAtual;
    }

    public int getMaiorOcupacao() {
        return maiorOcupacao;
    }

    public boolean estaVazia() {
        return ocupacaoAtual == 0;
    }
}
