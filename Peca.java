import java.util.ArrayList;
import java.util.List;

public class Peca {
    public int id;
    public boolean fim;
    public boolean aprovada;
    public int numeroCaixa;
    public List<String> historico;

    public Peca(int id) {
        this(id, false);
    }

    public Peca(int id, boolean fim) {
        this.id = id;
        this.fim = fim;
        this.aprovada = false;
        this.numeroCaixa = 0;
        this.historico = new ArrayList<>();
    }

    public boolean isFim() {
        return fim;
    }

    public void adicionarEtapa(String etapa) {
        historico.add(etapa);
    }
}
