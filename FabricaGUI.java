import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FabricaGUI extends JFrame {

    // Estados possíveis de uma máquina
    public enum EstadoMaquina {
        OCIOSA("OCIOSA", new Color(70, 78, 95)),
        AGUARDANDO_EXAUSTOR("AGUARDA EXAUSTOR", new Color(230, 81, 0)),
        PROCESSANDO("OPERANDO", new Color(0, 230, 118)),
        AGUARDANDO_SAIDA("AGUARDA SAIDA", new Color(255, 214, 0)),
        ENCERRADA("ENCERRADA", new Color(38, 50, 56));

        final String label;
        final Color cor;

        EstadoMaquina(String label, Color cor) {
            this.label = label;
            this.cor = cor;
        }
    }

    // Informação de estado de cada máquina
    static class InfoMaquina {
        String nome;
        String setor;
        EstadoMaquina estado = EstadoMaquina.OCIOSA;
        int pecaIdAtual = -1;
        int totalProcessado = 0;
        long tempoInicioOperacao = 0;
        int x, y, largura, altura;

        InfoMaquina(String nome, String setor, int x, int y, int largura, int altura) {
            this.nome = nome;
            this.setor = setor;
            this.x = x;
            this.y = y;
            this.largura = largura;
            this.altura = altura;
        }
    }

    // Peça visual para animação
    static class PecaVisual {
        int id;
        double x, y;
        double targetX, targetY;
        Color cor;
        boolean descartada = false;
        boolean embalada = false;

        PecaVisual(int id, double x, double y, Color cor) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.cor = cor;
        }
    }

    // Dados visuais da fábrica
    private final Map<String, InfoMaquina> maquinas = new LinkedHashMap<>();
    private final List<PecaVisual> pecasVisuais = new CopyOnWriteArrayList<>();
    private final List<String> ultimosLogs = new CopyOnWriteArrayList<>();
    private final List<List<Integer>> caixasFechadas = new CopyOnWriteArrayList<>();
    private final List<Integer> pecasCaixaAtual = new CopyOnWriteArrayList<>();
    private final List<Integer> pecasDescartadas = new CopyOnWriteArrayList<>();

    // Estado do Exaustor
    private String exaustorDono = "LIVRE"; // "LIVRE", "S1", "P1"
    private float exaustorAngulo = 0f;

    // Estatísticas
    private int totalCriadas = 0;
    private int totalAprovadas = 0;
    private int totalDescartadas = 0;
    private int totalEmbaladas = 0;
    private long tempoSimulacaoMs = 0;
    private boolean emExecucao = false;
    private Thread threadSimulacao;

    // Controles visuais
    private JComboBox<String> comboCenarios;
    private JButton btnIniciar;
    private JButton btnReiniciar;
    private JSlider sliderVelocidade;
    private JLabel lblStatusGeral;
    private PainelFabrica painelFabrica;

    private int tickAnimacao = 0;

    public FabricaGUI() {
        super("Simulador Industrial Concorrente — SO II (Edição Pixel Art)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 780);
        setLocationRelativeTo(null);
        setResizable(false);

        inicializarMaquinas();
        configurarInterface();
        conectarListenerRelatorio();

        // Timer de animação e repintura a ~60 FPS
        javax.swing.Timer timer = new javax.swing.Timer(16, this::atualizarAnimacao);
        timer.start();
    }

    private void inicializarMaquinas() {
        maquinas.clear();
        // Layout das estações de trabalho
        // Entrada: X ~ 40
        // Corte: X = 180 (C1 no topo Y=220, C2 embaixo Y=360)
        maquinas.put("C1", new InfoMaquina("C1", "Corte", 170, 210, 110, 95));
        maquinas.put("C2", new InfoMaquina("C2", "Corte", 170, 340, 110, 95));

        // Furação: X = 360 (F1 centro Y=275)
        maquinas.put("F1", new InfoMaquina("F1", "Furacao", 350, 275, 110, 95));

        // Soldagem: X = 530 (S1 centro Y=275)
        maquinas.put("S1", new InfoMaquina("S1", "Soldagem", 520, 275, 110, 95));

        // Pintura: X = 700 (P1 centro Y=275)
        maquinas.put("P1", new InfoMaquina("P1", "Pintura", 690, 275, 110, 95));

        // Inspeção: X = 870 (I1 topo Y=210, I2 embaixo Y=340)
        maquinas.put("I1", new InfoMaquina("I1", "Inspecao", 860, 210, 110, 95));
        maquinas.put("I2", new InfoMaquina("I2", "Inspecao", 860, 340, 110, 95));

        // Embalagem: X = 1040 (E1 centro Y=275)
        maquinas.put("E1", new InfoMaquina("E1", "Embalagem", 1030, 275, 120, 95));
    }

    private JButton criarBotaoArcade(String texto, Color corFundo, Color corTexto) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(45, 52, 65));
                } else if (getModel().isPressed()) {
                    g2.setColor(corFundo.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(corFundo.brighter());
                } else {
                    g2.setColor(corFundo);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setForeground(corTexto);
        btn.setFont(new Font("Consolas", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private void configurarInterface() {
        JPanel painelPrincipal = new JPanel(new BorderLayout());
        painelPrincipal.setBackground(new Color(18, 20, 28));

        // Barra Superior de Controles Arcade
        JPanel barraControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        barraControles.setBackground(new Color(24, 28, 40));
        barraControles.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(42, 50, 72)));

        JLabel lblCenario = new JLabel("CENARIO:");
        lblCenario.setForeground(new Color(0, 229, 255));
        lblCenario.setFont(new Font("Consolas", Font.BOLD, 13));

        String[] cenariosNomes = {
                "1: Fluxo Completo (Padrao - 24 pecas)",
                "2: Armazenamento Reduzido (Filas = 1)",
                "3: Inspecao Lenta (I1=2.5s, I2=3.0s)",
                "4: Disputa Exaustor (600ms)",
                "5: Carga Ampliada (48 pecas - Turbo 0.1x)"
        };
        comboCenarios = new JComboBox<>(cenariosNomes);
        comboCenarios.setBackground(new Color(24, 30, 44));
        comboCenarios.setForeground(Color.WHITE);
        comboCenarios.setFont(new Font("Consolas", Font.BOLD, 12));
        comboCenarios.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    lbl.setBackground(new Color(0, 229, 255));
                    lbl.setForeground(Color.BLACK);
                } else {
                    lbl.setBackground(new Color(24, 30, 44));
                    lbl.setForeground(new Color(240, 245, 250));
                }
                lbl.setOpaque(true);
                lbl.setFont(new Font("Consolas", Font.BOLD, 12));
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                return lbl;
            }
        });

        btnIniciar = criarBotaoArcade("▶ INICIAR SIMULACAO", new Color(0, 230, 118), Color.BLACK);
        btnIniciar.addActionListener(e -> iniciarSimulacao());

        btnReiniciar = criarBotaoArcade("↺ REINICIAR", new Color(255, 112, 67), Color.BLACK);
        btnReiniciar.addActionListener(e -> reiniciarSimulacao());

        JLabel lblVel = new JLabel("VELOCIDADE:");
        lblVel.setForeground(new Color(255, 214, 0));
        lblVel.setFont(new Font("Consolas", Font.BOLD, 12));

        // Slider de velocidade: 5 (0.5x - Câmera Lenta) a 50 (5.0x - Turbo)
        sliderVelocidade = new JSlider(5, 50, 10);
        sliderVelocidade.setBackground(new Color(24, 28, 40));
        sliderVelocidade.setForeground(Color.WHITE);
        sliderVelocidade.setPreferredSize(new Dimension(110, 25));

        JLabel lblVelocidadeValor = new JLabel("1.0x");
        lblVelocidadeValor.setForeground(new Color(0, 229, 255));
        lblVelocidadeValor.setFont(new Font("Consolas", Font.BOLD, 12));
        lblVelocidadeValor.setPreferredSize(new Dimension(38, 25));

        sliderVelocidade.addChangeListener(e -> {
            double mult = sliderVelocidade.getValue() / 10.0;
            lblVelocidadeValor.setText(String.format(Locale.US, "%.1fx", mult));
            Configuracao.MULTIPLICADOR_VELOCIDADE = mult;
        });

        lblStatusGeral = new JLabel("STATUS: PRONTO");
        lblStatusGeral.setForeground(new Color(176, 190, 197));
        lblStatusGeral.setFont(new Font("Consolas", Font.BOLD, 13));

        barraControles.add(lblCenario);
        barraControles.add(comboCenarios);
        barraControles.add(btnIniciar);
        barraControles.add(btnReiniciar);
        barraControles.add(lblVel);
        barraControles.add(sliderVelocidade);
        barraControles.add(lblVelocidadeValor);
        barraControles.add(lblStatusGeral);

        painelFabrica = new PainelFabrica();
        painelPrincipal.add(barraControles, BorderLayout.NORTH);
        painelPrincipal.add(painelFabrica, BorderLayout.CENTER);

        setContentPane(painelPrincipal);
    }

    private void conectarListenerRelatorio() {
        Relatorio.setListener(new Relatorio.Listener() {
            @Override
            public void onLog(long tempoMs, String maquina, int pecaId, String evento) {
                SwingUtilities.invokeLater(() -> processarEventoLog(tempoMs, maquina, pecaId, evento));
            }

            @Override
            public void onAprovada(int id) {
                SwingUtilities.invokeLater(() -> {
                    totalAprovadas++;
                });
            }

            @Override
            public void onDescartada(int id) {
                SwingUtilities.invokeLater(() -> {
                    totalDescartadas++;
                    pecasDescartadas.add(id);
                });
            }

            @Override
            public void onCaixa(List<Integer> pecasCaixa, int numeroCaixa) {
                SwingUtilities.invokeLater(() -> {
                    caixasFechadas.add(new ArrayList<>(pecasCaixa));
                    totalEmbaladas += pecasCaixa.size();
                    pecasCaixaAtual.clear();
                });
            }
        });
    }

    private void processarEventoLog(long tempoMs, String maquina, int pecaId, String evento) {
        tempoSimulacaoMs = tempoMs;

        // Formata e guarda log no ticker inferior
        String linhaLog = String.format("[%05d ms] [%-7s] [Peca %-3s] %s",
                tempoMs, maquina, (pecaId == -1 ? "FIM" : pecaId), evento);
        ultimosLogs.add(linhaLog);
        if (ultimosLogs.size() > 5) {
            ultimosLogs.remove(0);
        }

        if ("Fabrica".equals(maquina) && evento.contains("PECA GERADA")) {
            totalCriadas++;
            // Cria peça visual no início da esteira
            PecaVisual pv = new PecaVisual(pecaId, 50, 310, new Color(79, 195, 247));
            pv.targetX = 140;
            pv.targetY = 310;
            pecasVisuais.add(pv);
            return;
        }

        InfoMaquina info = maquinas.get(maquina);
        if (info != null) {
            if (evento.contains("RECEBEU FIM")) {
                info.estado = EstadoMaquina.ENCERRADA;
                info.pecaIdAtual = -1;
            } else if (evento.contains("ADQUIRIU EXAUSTOR")) {
                info.estado = EstadoMaquina.PROCESSANDO;
                exaustorDono = maquina;
            } else if (evento.contains("LIBEROU EXAUSTOR")) {
                if (exaustorDono.equals(maquina)) {
                    exaustorDono = "LIVRE";
                }
            } else if (evento.contains("INICIO OPERACAO")) {
                info.estado = EstadoMaquina.PROCESSANDO;
                info.pecaIdAtual = pecaId;
                info.tempoInicioOperacao = System.currentTimeMillis();
                info.totalProcessado++;

                if ("E1".equals(maquina) && pecaId != -1) {
                    pecasCaixaAtual.add(pecaId);
                }
            } else if (evento.contains("FIM OPERACAO")) {
                // Máquina terminou e aguarda vaga ou liberação
            } else if (evento.contains("AGUARDANDO SAIDA")) {
                info.estado = EstadoMaquina.AGUARDANDO_SAIDA;
            } else if (evento.contains("REPROVADA NA INSPECAO")) {
                info.estado = EstadoMaquina.AGUARDANDO_SAIDA;
            } else if (evento.contains("APROVADA NA INSPECAO")) {
                info.estado = EstadoMaquina.AGUARDANDO_SAIDA;
            }
        }
    }

    private void iniciarSimulacao() {
        if (emExecucao) return;
        emExecucao = true;
        btnIniciar.setEnabled(false);
        comboCenarios.setEnabled(false);
        lblStatusGeral.setText("STATUS: EXECUTANDO LINHA...");
        lblStatusGeral.setForeground(new Color(0, 230, 118));

        reiniciarDadosVisual();

        int cenarioIndex = comboCenarios.getSelectedIndex() + 1;
        Configuracao.MULTIPLICADOR_VELOCIDADE = sliderVelocidade.getValue() / 10.0;

        threadSimulacao = new Thread(() -> {
            Fabrica.executarCenario(cenarioIndex);
            SwingUtilities.invokeLater(() -> {
                emExecucao = false;
                btnIniciar.setEnabled(true);
                comboCenarios.setEnabled(true);
                lblStatusGeral.setText("STATUS: FINALIZADO COM SUCESSO");
                lblStatusGeral.setForeground(new Color(0, 229, 255));
                exibirRelatorioConclusao();
            });
        }, "Thread-Fabrica-GUI");
        threadSimulacao.start();
    }

    private void reiniciarSimulacao() {
        if (threadSimulacao != null && threadSimulacao.isAlive()) {
            threadSimulacao.interrupt();
        }
        emExecucao = false;
        btnIniciar.setEnabled(true);
        comboCenarios.setEnabled(true);
        lblStatusGeral.setText("STATUS: REINICIADO");
        lblStatusGeral.setForeground(new Color(176, 190, 197));
        Configuracao.MULTIPLICADOR_VELOCIDADE = sliderVelocidade.getValue() / 10.0;
        reiniciarDadosVisual();
        repaint();
    }

    private void reiniciarDadosVisual() {
        totalCriadas = 0;
        totalAprovadas = 0;
        totalDescartadas = 0;
        totalEmbaladas = 0;
        tempoSimulacaoMs = 0;
        exaustorDono = "LIVRE";
        pecasVisuais.clear();
        ultimosLogs.clear();
        caixasFechadas.clear();
        pecasCaixaAtual.clear();
        pecasDescartadas.clear();
        inicializarMaquinas();
    }

    private void exibirRelatorioConclusao() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== SIMULACAO CONCLUIDA ==========\n");
        sb.append("Pecas Criadas: ").append(totalCriadas).append("\n");
        sb.append("Pecas Aprovadas: ").append(totalAprovadas).append("\n");
        sb.append("Pecas Descartadas: ").append(totalDescartadas).append(" -> ").append(pecasDescartadas).append("\n");
        sb.append("Pecas Embaladas: ").append(totalEmbaladas).append("\n");
        sb.append("Total de Caixas: ").append(caixasFechadas.size()).append("\n");
        for (int i = 0; i < caixasFechadas.size(); i++) {
            sb.append("  Caixa ").append(i + 1).append(": ").append(caixasFechadas.get(i)).append("\n");
        }
        sb.append("Tempo Total: ").append(tempoSimulacaoMs).append(" ms\n");
        sb.append("=========================================");

        JOptionPane.showMessageDialog(this, sb.toString(), "Relatorio Final da Fabrica", JOptionPane.INFORMATION_MESSAGE);
    }

    private void atualizarAnimacao(ActionEvent e) {
        tickAnimacao++;
        exaustorAngulo += 0.15f;
        painelFabrica.repaint();
    }

    // Painel customizado de desenho Pixel Art
    class PainelFabrica extends JPanel {
        public PainelFabrica() {
            setBackground(new Color(16, 18, 25));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Renderização pixel art nítida
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            desenharPisoIndustrial(g2);
            desenharDutoExaustor(g2);
            desenharEsteiras(g2);
            desenharFilasBuffer(g2);
            desenharMaquinas(g2);
            desenharAreaDescarte(g2);
            desenharAreaEmbalagem(g2);
            desenharPlacarArcade(g2);
            desenharTerminalLogs(g2);
        }

        private void desenharPisoIndustrial(Graphics2D g2) {
            // Grade de azulejos industriais pixelados
            g2.setColor(new Color(22, 25, 35));
            int tileSize = 32;
            for (int x = 0; x < getWidth(); x += tileSize) {
                for (int y = 0; y < getHeight(); y += tileSize) {
                    g2.drawRect(x, y, tileSize, tileSize);
                }
            }
        }

        private void desenharEsteiras(Graphics2D g2) {
            int yEsteira = 310;
            int alturaEsteira = 30;

            // Corpo da esteira rolante principal
            g2.setColor(new Color(40, 44, 56));
            g2.fillRect(30, yEsteira, 1140, alturaEsteira);

            // Borda metálica da esteira com rebites
            g2.setColor(new Color(75, 85, 105));
            g2.fillRect(30, yEsteira - 4, 1140, 4);
            g2.fillRect(30, yEsteira + alturaEsteira, 1140, 4);

            // Listras de advertência animadas (amarelo e preto)
            g2.setColor(new Color(255, 214, 0, 180));
            int offset = (tickAnimacao * 2) % 24;
            Shape clipAntigo = g2.getClip();
            g2.setClip(30, yEsteira, 1140, alturaEsteira);

            for (int x = 30 - 24 + offset; x < 1200; x += 24) {
                int[] px = {x, x + 10, x + 4, x - 6};
                int[] py = {yEsteira, yEsteira, yEsteira + alturaEsteira, yEsteira + alturaEsteira};
                g2.fillPolygon(px, py, 4);
            }
            g2.setClip(clipAntigo);

            // Chute de descarte descendo da Inspeção
            g2.setColor(new Color(60, 20, 25));
            int[] chuteX = {910, 935, 935, 910};
            int[] chuteY = {340, 340, 490, 490};
            g2.fillPolygon(chuteX, chuteY, 4);
            g2.setColor(new Color(213, 0, 0));
            g2.drawPolygon(chuteX, chuteY, 4);
        }

        private void desenharDutoExaustor(Graphics2D g2) {
            // O duto conecta S1 (X~575) e P1 (X~745) passando por um exaustor central (X=660, Y=140)
            int yDuto = 145;
            int xS1 = 575;
            int xP1 = 745;
            int xCentro = (xS1 + xP1) / 2;

            // Tubulações metálicas
            g2.setColor(new Color(45, 55, 72));
            g2.fillRect(xS1 - 10, yDuto, 20, 130); // Descida para S1
            g2.fillRect(xP1 - 10, yDuto, 20, 130); // Descida para P1
            g2.fillRect(xS1 - 10, yDuto - 10, (xP1 - xS1) + 20, 20); // Tubo horizontal

            // Destaque de fluxo de ar quando ocupado
            if (!"LIVRE".equals(exaustorDono)) {
                g2.setColor(new Color(0, 229, 255, 120));
                if ("S1".equals(exaustorDono)) {
                    g2.fillRect(xS1 - 6, yDuto, 12, 130);
                    g2.fillRect(xS1, yDuto - 6, xCentro - xS1, 12);
                } else if ("P1".equals(exaustorDono)) {
                    g2.fillRect(xP1 - 6, yDuto, 12, 130);
                    g2.fillRect(xCentro, yDuto - 6, xP1 - xCentro, 12);
                }
            }

            // Ventilador / Carcaça do Exaustor no centro
            int raio = 26;
            g2.setColor(new Color(30, 35, 48));
            g2.fillOval(xCentro - raio, yDuto - raio + 5, raio * 2, raio * 2);
            g2.setColor(new Color(0, 229, 255));
            g2.drawOval(xCentro - raio, yDuto - raio + 5, raio * 2, raio * 2);

            // Pás do ventilador girando
            g2.setColor(new Color(128, 222, 234));
            for (int i = 0; i < 4; i++) {
                double a = exaustorAngulo + (i * Math.PI / 2);
                int px = (int) (xCentro + Math.cos(a) * (raio - 6));
                int py = (int) ((yDuto + 5) + Math.sin(a) * (raio - 6));
                g2.drawLine(xCentro, yDuto + 5, px, py);
            }

            // Placa LED do Exaustor Compartilhado
            g2.setColor(new Color(15, 18, 26));
            g2.fillRect(xCentro - 75, yDuto - 52, 150, 26);
            g2.setColor(new Color(75, 85, 105));
            g2.drawRect(xCentro - 75, yDuto - 52, 150, 26);

            Color corStatus;
            String textoStatus;
            if ("LIVRE".equals(exaustorDono)) {
                corStatus = new Color(0, 230, 118);
                textoStatus = "EXAUSTOR: LIVRE";
            } else {
                corStatus = new Color(255, 61, 0);
                textoStatus = "EXAUSTOR: EM USO (" + exaustorDono + ")";
            }
            g2.setColor(corStatus);
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            int tw = g2.getFontMetrics().stringWidth(textoStatus);
            g2.drawString(textoStatus, xCentro - (tw / 2), yDuto - 35);
        }

        private void desenharFilasBuffer(Graphics2D g2) {
            List<FilaLimitada> filas = Fabrica.filasAtivas;
            if (filas == null) return;

            int[] posXFilas = {130, 310, 485, 655, 825, 995};

            for (int i = 0; i < filas.size() && i < posXFilas.length; i++) {
                FilaLimitada f = filas.get(i);
                int x = posXFilas[i];
                int y = 445;

                // Caixa do buffer
                g2.setColor(new Color(24, 28, 38));
                g2.fillRect(x - 25, y, 50, 38);
                g2.setColor(new Color(60, 70, 90));
                g2.drawRect(x - 25, y, 50, 38);

                // Título
                g2.setColor(new Color(144, 164, 174));
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                String abrev = f.getNome().replace("Fila-", "");
                g2.drawString(abrev, x - (g2.getFontMetrics().stringWidth(abrev) / 2), y + 12);

                // Slots de capacidade
                int cap = f.getCapacidade();
                int ocup = f.getOcupacaoAtual();

                int slotW = 6;
                int startX = x - (cap * (slotW + 2)) / 2;
                for (int s = 0; s < cap; s++) {
                    if (s < ocup) {
                        g2.setColor(new Color(255, 214, 0)); // Ocupado
                        g2.fillRect(startX + s * (slotW + 2), y + 18, slotW, 10);
                    } else {
                        g2.setColor(new Color(40, 45, 60)); // Vazio
                        g2.drawRect(startX + s * (slotW + 2), y + 18, slotW, 10);
                    }
                }

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                String qtd = ocup + "/" + cap;
                g2.drawString(qtd, x - (g2.getFontMetrics().stringWidth(qtd) / 2), y + 35);
            }
        }

        private void desenharMaquinas(Graphics2D g2) {
            for (InfoMaquina m : maquinas.values()) {
                desenharUnidadeMaquina(g2, m);
            }
        }

        private void desenharUnidadeMaquina(Graphics2D g2, InfoMaquina m) {
            // Corpo da máquina com chanfro pixelado
            g2.setColor(new Color(28, 32, 45));
            g2.fillRect(m.x, m.y, m.largura, m.altura);

            // Borda de status com iluminação
            g2.setColor(m.estado.cor);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(m.x, m.y, m.largura, m.altura);
            g2.setStroke(new BasicStroke(1));

            // Cabeçalho da máquina
            g2.setColor(new Color(40, 48, 68));
            g2.fillRect(m.x + 2, m.y + 2, m.largura - 4, 20);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.drawString(m.nome + " (" + m.setor + ")", m.x + 8, m.y + 16);

            // LED de Status
            g2.setColor(m.estado.cor);
            g2.fillOval(m.x + m.largura - 16, m.y + 6, 8, 8);

            // Janela de processamento interna
            int jX = m.x + 10;
            int jY = m.y + 28;
            int jW = m.largura - 20;
            int jH = 40;

            g2.setColor(new Color(15, 18, 26));
            g2.fillRect(jX, jY, jW, jH);
            g2.setColor(new Color(50, 60, 80));
            g2.drawRect(jX, jY, jW, jH);

            // Se estiver processando, desenha efeitos pixelados
            if (m.estado == EstadoMaquina.PROCESSANDO) {
                desenharEfeitoOperacao(g2, m, jX, jY, jW, jH);
            }

            // Exibição da peça atual se houver
            if (m.pecaIdAtual > 0) {
                desenharBlocoPeca(g2, jX + (jW / 2) - 14, jY + (jH / 2) - 10, m.pecaIdAtual,
                        (m.pecaIdAtual % 7 == 0 && m.setor.startsWith("Insp")) ? new Color(255, 23, 68) : new Color(0, 229, 255));
            }

            // Texto de status e total processado no rodapé
            g2.setColor(m.estado.cor);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.drawString(m.estado.label, m.x + 8, m.y + m.altura - 14);

            g2.setColor(new Color(176, 190, 197));
            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.drawString("Total: " + m.totalProcessado, m.x + 8, m.y + m.altura - 4);
        }

        private void desenharEfeitoOperacao(Graphics2D g2, InfoMaquina m, int x, int y, int w, int h) {
            int frame = (tickAnimacao / 3) % 4;

            if (m.nome.startsWith("C")) {
                // Efeito de Faíscas de Corte
                g2.setColor(new Color(255, 235, 59));
                int sparkX = x + (tickAnimacao * 5) % w;
                g2.drawLine(sparkX, y + 5, sparkX + 4, y + 15);
                g2.drawLine(sparkX + 2, y + 10, sparkX - 3, y + 25);
            } else if (m.nome.startsWith("F")) {
                // Broca de Furação descendo
                g2.setColor(new Color(176, 190, 197));
                int drillY = y + 5 + (frame * 3);
                g2.fillRect(x + (w / 2) - 3, y + 2, 6, drillY - y);
            } else if (m.nome.startsWith("S")) {
                // Faíscas intensas de soldagem e clarão
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillOval(x + (w / 2) - 10, y + (h / 2) - 10, 20, 20);
                g2.setColor(new Color(255, 145, 0));
                for (int i = 0; i < 5; i++) {
                    int rx = (int) (Math.random() * w);
                    int ry = (int) (Math.random() * h);
                    g2.fillRect(x + rx, y + ry, 2, 2);
                }
            } else if (m.nome.startsWith("P")) {
                // Névoa de spray de pintura
                g2.setColor(new Color(0, 229, 255, 140));
                for (int i = 0; i < 8; i++) {
                    int px = x + 5 + (i * 4);
                    int py = y + 10 + ((i + frame) % 5) * 4;
                    g2.fillOval(px, py, 3, 3);
                }
            } else if (m.nome.startsWith("I")) {
                // Laser scanner vertical
                int scanX = x + 4 + ((tickAnimacao * 3) % (w - 8));
                g2.setColor(new Color(255, 23, 68));
                g2.drawLine(scanX, y + 2, scanX, y + h - 2);
            }
        }

        private void desenharAreaDescarte(Graphics2D g2) {
            // Caçamba de descarte abaixo da inspeção
            int x = 880;
            int y = 490;
            int w = 110;
            int h = 60;

            g2.setColor(new Color(40, 15, 20));
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(255, 23, 68));
            g2.drawRect(x, y, w, h);

            // Símbolo de lixeira / descarte
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("SUCATA / DESCARTE", x + 8, y + 15);
            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.setColor(new Color(239, 154, 154));
            g2.drawString("(IDs % 7 == 0)", x + 18, y + 28);

            // Desenha as peças descartadas acumuladas
            g2.drawString("Descartadas: " + totalDescartadas, x + 8, y + 43);
            if (!pecasDescartadas.isEmpty()) {
                g2.drawString(pecasDescartadas.toString(), x + 8, y + 54);
            }
        }

        private void desenharAreaEmbalagem(Graphics2D g2) {
            // Caixa aberta atual e caixas fechadas ao lado de E1
            int x = 1040;
            int y = 390;
            int w = 110;
            int h = 75;

            // Caixa de papelão pixelada aberta
            g2.setColor(new Color(62, 39, 35));
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(141, 110, 99));
            g2.drawRect(x, y, w, h);

            g2.setColor(new Color(255, 204, 128));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("CAIXA ATUAL (" + pecasCaixaAtual.size() + "/4)", x + 8, y + 16);

            // 4 slots da caixa
            for (int s = 0; s < 4; s++) {
                int sx = x + 12 + (s * 22);
                int sy = y + 25;
                if (s < pecasCaixaAtual.size()) {
                    desenharBlocoPeca(g2, sx, sy, pecasCaixaAtual.get(s), new Color(0, 229, 255));
                } else {
                    g2.setColor(new Color(93, 64, 55));
                    g2.drawRect(sx, sy, 18, 18);
                }
            }

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.drawString("Caixas fechadas: " + caixasFechadas.size(), x + 8, y + 65);
        }

        private void desenharBlocoPeca(Graphics2D g2, int x, int y, int id, Color cor) {
            // Suporte metálico pixelado
            g2.setColor(cor);
            g2.fillRect(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, 20, 20);

            // Número do ID
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            String texto = String.valueOf(id);
            int tw = g2.getFontMetrics().stringWidth(texto);
            g2.drawString(texto, x + 10 - (tw / 2), y + 14);
        }

        private void desenharPlacarArcade(Graphics2D g2) {
            // Placar estilo máquina Arcade no topo da tela
            int x = 30;
            int y = 50;
            int w = 1180;
            int h = 60;

            g2.setColor(new Color(20, 24, 34));
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(0, 229, 255));
            g2.drawRect(x, y, w, h);

            g2.setFont(new Font("Consolas", Font.BOLD, 13));

            // Indicadores do placar
            desenharItemPlacar(g2, x + 30, y + 36, "CRIADAS", String.valueOf(totalCriadas), Color.WHITE);
            desenharItemPlacar(g2, x + 180, y + 36, "APROVADAS", String.valueOf(totalAprovadas), new Color(0, 230, 118));
            desenharItemPlacar(g2, x + 350, y + 36, "DESCARTADAS", String.valueOf(totalDescartadas), new Color(255, 23, 68));
            desenharItemPlacar(g2, x + 530, y + 36, "EMBALADAS", String.valueOf(totalEmbaladas), new Color(0, 229, 255));
            desenharItemPlacar(g2, x + 710, y + 36, "CAIXAS FECHADAS", String.valueOf(caixasFechadas.size()), new Color(255, 214, 0));
            desenharItemPlacar(g2, x + 930, y + 36, "TEMPO", tempoSimulacaoMs + " ms", new Color(178, 255, 89));
        }

        private void desenharItemPlacar(Graphics2D g2, int x, int y, String rotulo, String valor, Color corValor) {
            g2.setColor(new Color(144, 164, 174));
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.drawString(rotulo, x, y - 14);

            g2.setColor(corValor);
            g2.setFont(new Font("Consolas", Font.BOLD, 16));
            g2.drawString(valor, x, y + 6);
        }

        private void desenharTerminalLogs(Graphics2D g2) {
            // Ticker de logs em tempo real na parte inferior
            int x = 30;
            int y = 575;
            int w = 1180;
            int h = 110;

            g2.setColor(new Color(10, 12, 18));
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(42, 50, 72));
            g2.drawRect(x, y, w, h);

            g2.setColor(new Color(0, 229, 255));
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.drawString(">>> CONSOLE DE EVENTOS EM TEMPO REAL (THREADS & SEMAFOROS):", x + 12, y + 18);

            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            int logY = y + 36;
            for (String log : ultimosLogs) {
                if (log.contains("REPROVADA") || log.contains("ENCERRANDO")) {
                    g2.setColor(new Color(255, 82, 82));
                } else if (log.contains("EXAUSTOR")) {
                    g2.setColor(new Color(255, 215, 64));
                } else if (log.contains("APROVADA") || log.contains("CAIXA")) {
                    g2.setColor(new Color(105, 240, 174));
                } else {
                    g2.setColor(new Color(207, 216, 220));
                }
                g2.drawString(log, x + 15, logY);
                logY += 16;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                UIManager.put("ComboBox.background", new Color(24, 30, 44));
                UIManager.put("ComboBox.foreground", Color.WHITE);
                UIManager.put("ComboBox.selectionBackground", new Color(0, 229, 255));
                UIManager.put("ComboBox.selectionForeground", Color.BLACK);
            } catch (Exception ignored) {
            }
            FabricaGUI gui = new FabricaGUI();
            gui.setVisible(true);
        });
    }
}
