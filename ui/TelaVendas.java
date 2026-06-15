package ui;

import exceptions.EstoqueInsuficienteException;
import exceptions.VendaFinalizadaException;
import model.*;
import service.Sistema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TelaVendas extends JPanel {

    private final Sistema sistema;
    private DefaultTableModel modeloVendas;
    private JTable tabelaVendas;

    public TelaVendas(Sistema sistema) {
        this.sistema = sistema;
        setBackground(TelaPrincipal.COR_FUNDO);
        setLayout(new BorderLayout());
        construir();
    }

    private void construir() {
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(TelaPrincipal.COR_FUNDO);
        cabecalho.setBorder(new EmptyBorder(28, 32, 16, 32));
        cabecalho.add(TelaPrincipal.labelTitulo("Vendas"), BorderLayout.WEST);
        JButton btnNova = TelaPrincipal.botaoPrimario("+ Nova Venda");
        btnNova.addActionListener(e -> abrirNovaVenda());
        cabecalho.add(btnNova, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);

        String[] colunas = {"#", "Cliente", "Funcionário", "Data", "Total", "Status"};
        modeloVendas = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaVendas = new JTable(modeloVendas);
        estilizarTabela(tabelaVendas);
        atualizarTabela();

        JScrollPane scroll = new JScrollPane(tabelaVendas);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        // ── Barra de ações ────────────────────────────────────────────────────
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        acoes.setBackground(Color.WHITE);
        acoes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnDetalhes = new JButton("Ver Detalhes");
        JButton btnNota     = new JButton("Emitir Nota Fiscal");
        estilizarBtnSecundario(btnDetalhes);
        estilizarBtnSecundario(btnNota);

        btnDetalhes.addActionListener(e -> {
            int row = tabelaVendas.getSelectedRow();
            if (row < 0) { avisar("Selecione uma venda."); return; }
            Vendas v = sistema.getVendas().get(row);
            exibirTexto("Detalhes da Venda", v.gerarRelatorio());
        });

        btnNota.addActionListener(e -> {
            int row = tabelaVendas.getSelectedRow();
            if (row < 0) { avisar("Selecione uma venda para emitir a nota."); return; }
            Vendas v = sistema.getVendas().get(row);
            String descInput = JOptionPane.showInputDialog(this,
                "Desconto em % (0 para nenhum):", "Nota Fiscal", JOptionPane.PLAIN_MESSAGE);
            if (descInput == null) return;
        });

        acoes.add(btnDetalhes);
        acoes.add(btnNota);

        JPanel painelTabela = new JPanel(new BorderLayout());
        painelTabela.setBorder(new EmptyBorder(0, 32, 32, 32));
        painelTabela.setBackground(TelaPrincipal.COR_FUNDO);
        painelTabela.add(scroll, BorderLayout.CENTER);
        painelTabela.add(acoes, BorderLayout.SOUTH);
        add(painelTabela, BorderLayout.CENTER);
    }

    private void abrirNovaVenda() {
        if (sistema.getClientes().isEmpty() || sistema.getFuncionarios().isEmpty()
                || sistema.getProdutos().isEmpty()) {
            avisar("Cadastre ao menos 1 cliente, 1 funcionário e 1 produto antes de realizar uma venda.");
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Nova Venda", true);
        dialog.setSize(660, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Color.WHITE);

        // ── Seleção de cliente e funcionário ──────────────────────────────────
        JPanel topoForm = new JPanel(new GridLayout(2, 2, 10, 10));
        topoForm.setBackground(Color.WHITE);
        topoForm.setBorder(new EmptyBorder(20, 20, 10, 20));

        JComboBox<String> cbCliente = new JComboBox<>();
        for (Clientes c : sistema.getClientes()) cbCliente.addItem(c.getNome());

        JComboBox<String> cbFuncionario = new JComboBox<>();
        for (Funcionario f : sistema.getFuncionarios()) cbFuncionario.addItem(f.getNome());

        topoForm.add(rotulo("Cliente:")); topoForm.add(cbCliente);
        topoForm.add(rotulo("Funcionário:")); topoForm.add(cbFuncionario);
        dialog.add(topoForm, BorderLayout.NORTH);

        // ── Tabela de itens ───────────────────────────────────────────────────
        String[] colsItens = {"Produto", "Preço Unit.", "Qtd", "Subtotal"};
        DefaultTableModel modeloItens = new DefaultTableModel(colsItens, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaItens = new JTable(modeloItens);
        estilizarTabela(tabelaItens); 

        // ── Painel add produto ────────────────────────────────────────────────
        JPanel addProduto = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        addProduto.setBackground(new Color(248, 250, 252));
        addProduto.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(226, 232, 240)));

        JComboBox<String> cbProduto = new JComboBox<>();
        for (Produtos p : sistema.getProdutos())
            cbProduto.addItem(p.getCodigo() + " - " + p.getNome() + " (Est: " + p.getEstoque() + ")");
        cbProduto.setPreferredSize(new Dimension(180, 32));

        JSpinner spinQtd = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        spinQtd.setPreferredSize(new Dimension(70, 32));

        JButton btnAdd    = TelaPrincipal.botaoPrimario("+ Adicionar");
        JButton btnRemover = TelaPrincipal.botaoPerigo("− Remover");

        addProduto.add(rotulo("Produto:")); addProduto.add(cbProduto);
        addProduto.add(rotulo("Qtd:"));     addProduto.add(spinQtd);
        addProduto.add(btnAdd);             addProduto.add(btnRemover);

        List<Produtos> itensSelecionados = new ArrayList<>();
        List<Integer>  quantidades       = new ArrayList<>();
        JLabel labelTotal = new JLabel("Total: R$ 0,00");
        labelTotal.setFont(new Font("SansSerif", Font.BOLD, 15));
        labelTotal.setForeground(TelaPrincipal.COR_PRIMARIA);

        btnAdd.addActionListener(e -> {
            int idx = cbProduto.getSelectedIndex();
            if (idx < 0) return;
            Produtos prod = sistema.getProdutos().get(idx);
            int qtd = (int) spinQtd.getValue();
            if (!prod.temEstoque(qtd)) {
                avisar("Estoque insuficiente! Disponível: " + prod.getEstoque());
                return;
            }
            itensSelecionados.add(prod);
            quantidades.add(qtd);
            modeloItens.addRow(new Object[]{
                prod.getNome(),
                String.format("R$ %.2f", prod.getPreco()),
                qtd,
                String.format("R$ %.2f", prod.getPreco() * qtd)
            });
            atualizarTotal(labelTotal, itensSelecionados, quantidades);
        });

        btnRemover.addActionListener(e -> {
            int row = tabelaItens.getSelectedRow();
            if (row < 0) { avisar("Selecione um item para remover."); return; }
            itensSelecionados.remove(row);
            quantidades.remove(row);
            modeloItens.removeRow(row);
            atualizarTotal(labelTotal, itensSelecionados, quantidades);
        });

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        centerPanel.add(addProduto, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        // ── Rodapé ────────────────────────────────────────────────────────────
        JButton btnFinalizar = TelaPrincipal.botaoSucesso("Finalizar Venda");
        JButton btnCancelar  = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());

        btnFinalizar.addActionListener(e -> {
            if (itensSelecionados.isEmpty()) {
                avisar("Adicione ao menos um produto.");
                return;
            }
            Clientes    cliente     = sistema.getClientes().get(cbCliente.getSelectedIndex());
            Funcionario funcionario = sistema.getFuncionarios().get(cbFuncionario.getSelectedIndex());

            Vendas venda = new Vendas(cliente, funcionario);
            for (int i = 0; i < itensSelecionados.size(); i++) {
                Produtos p = itensSelecionados.get(i);
                int qtd = quantidades.get(i);
                try {
                    sistema.reduzirEstoque(p, qtd);
                } catch (EstoqueInsuficienteException ex) {
                    avisar(ex.getMessage());
                    return;
                }
                for (int q = 0; q < qtd; q++) venda.adicionarProduto(p);
            }
            try {
                sistema.registrarVenda(venda);
                atualizarTabela();
                JOptionPane.showMessageDialog(dialog,
                    "Venda #" + venda.getId() + " registrada com sucesso!",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                
                dialog.dispose();
            } catch (VendaFinalizadaException ex) {
                avisar(ex.getMessage());
            }
        });

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(Color.WHITE);
        rodape.setBorder(new EmptyBorder(12, 20, 16, 20));
        rodape.add(labelTotal, BorderLayout.WEST);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(btnCancelar);
        btnPanel.add(btnFinalizar);
        rodape.add(btnPanel, BorderLayout.EAST);
        dialog.add(rodape, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void atualizarTotal(JLabel label, List<Produtos> itens, List<Integer> qtds) {
        double total = 0;
        for (int i = 0; i < itens.size(); i++)
            total += itens.get(i).getPreco() * qtds.get(i);
        label.setText(String.format("Total: R$ %.2f", total));
    }

    private void atualizarTabela() {
        modeloVendas.setRowCount(0);
        for (Vendas v : sistema.getVendas()) {
            modeloVendas.addRow(new Object[]{
                "#" + v.getId(),
                v.getCliente().getNome(),
                v.getFuncionario().getNome(),
                v.getDataVenda().toString(),
                String.format("R$ %.2f", v.calcularTotal()),
                v.getStatus()
            });
        }
    }

    private void estilizarTabela(JTable t) {
        t.setRowHeight(36);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(241, 245, 249));
        t.getTableHeader().setForeground(TelaPrincipal.COR_TEXTO_CLARO);
        t.setGridColor(new Color(226, 232, 240));
        t.setSelectionBackground(new Color(224, 231, 255));
        t.setBackground(Color.WHITE);
        t.setShowVerticalLines(false);
    }

    private void estilizarBtnSecundario(JButton btn) {
        btn.setBackground(new Color(241, 245, 249));
        btn.setForeground(TelaPrincipal.COR_TEXTO);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));
    }

    private void exibirTexto(String titulo, String conteudo) {
        JTextArea area = new JTextArea(conteudo);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(520, 400));
        JOptionPane.showMessageDialog(this, scroll, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    private JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(TelaPrincipal.COR_TEXTO);
        return l;
    }

    private void avisar(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }
}