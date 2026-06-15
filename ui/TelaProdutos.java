package ui;

import model.Produtos;
import service.Sistema;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class TelaProdutos extends JPanel {

    private final Sistema sistema;
    private DefaultTableModel modeloTabela;
    private JTable tabela;

    public TelaProdutos(Sistema sistema) {
        this.sistema = sistema;
        setBackground(TelaPrincipal.COR_FUNDO);
        setLayout(new BorderLayout());
        construir();
    }

    private void construir() {
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(TelaPrincipal.COR_FUNDO);
        cabecalho.setBorder(new EmptyBorder(28, 32, 16, 32));
        cabecalho.add(TelaPrincipal.labelTitulo("Produtos"), BorderLayout.WEST);
        JButton btnNovo = TelaPrincipal.botaoPrimario("+ Novo Produto");
        btnNovo.addActionListener(e -> abrirFormulario(-1));
        cabecalho.add(btnNovo, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);

        String[] colunas = {"Código", "Nome", "Preço", "Estoque", "Categoria"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tabela = new JTable(modeloTabela);
        estilizarTabela(tabela);
        atualizarTabela();

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        // ── Barra de ações ────────────────────────────────────────────────────
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        acoes.setBackground(Color.WHITE);
        acoes.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnEditar       = TelaPrincipal.botaoPrimario("✏ Editar");
        JButton btnExcluir      = TelaPrincipal.botaoPerigo("🗑 Excluir");
        JButton btnAjustarEstoque = new JButton("Ajustar Estoque");
        estilizarBtnSecundario(btnAjustarEstoque);

        btnEditar.addActionListener(e -> {
            int row = tabela.getSelectedRow();
            if (row < 0) { avisar("Selecione um produto para editar."); return; }
            abrirFormulario(row);
        });

        btnExcluir.addActionListener(e -> {
            int row = tabela.getSelectedRow();
            if (row < 0) { avisar("Selecione um produto para excluir."); return; }
            String nome = sistema.getProdutos().get(row).getNome();
            int confirm = JOptionPane.showConfirmDialog(this,
                "Excluir produto \"" + nome + "\"?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                sistema.getProdutos().remove(row);
                sistema.salvarTodosProdutos();
                atualizarTabela();
            }
        });

        btnAjustarEstoque.addActionListener(e -> {
            int row = tabela.getSelectedRow();
            if (row < 0) { avisar("Selecione um produto para ajustar o estoque."); return; }
            Produtos p = sistema.getProdutos().get(row);

            String input = JOptionPane.showInputDialog(this,
                "Estoque atual de \"" + p.getNome() + "\": " + p.getEstoque() +
                "\nNova quantidade:", "Ajustar Estoque", JOptionPane.PLAIN_MESSAGE);
            if (input != null && !input.isBlank()) {
                try {
                    int novoEstoque = Integer.parseInt(input.trim());
                    if (novoEstoque < 0) { avisar("Estoque não pode ser negativo."); return; }
                    p.setEstoque(novoEstoque);
                    sistema.salvarTodosProdutos();
                    atualizarTabela();
                    JOptionPane.showMessageDialog(this, "Estoque atualizado para " + novoEstoque + " unidades.");
                } catch (NumberFormatException ex) {
                    avisar("Digite um número válido.");
                }
            }
        });

        acoes.add(btnEditar);
        acoes.add(btnExcluir);
        acoes.add(btnAjustarEstoque);

        JPanel painelTabela = new JPanel(new BorderLayout());
        painelTabela.setBorder(new EmptyBorder(0, 32, 32, 32));
        painelTabela.setBackground(TelaPrincipal.COR_FUNDO);
        painelTabela.add(scroll, BorderLayout.CENTER);
        painelTabela.add(acoes, BorderLayout.SOUTH);
        add(painelTabela, BorderLayout.CENTER);
    }

    private void abrirFormulario(int rowEditar) {
        boolean editando = rowEditar >= 0;
        Produtos existente = editando ? sistema.getProdutos().get(rowEditar) : null;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                editando ? "Editar Produto" : "Novo Produto", true);
        dialog.setSize(400, 340);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 12));
        form.setBorder(new EmptyBorder(24, 24, 16, 24));
        form.setBackground(Color.WHITE);

        JTextField fCodigo    = campo(editando ? existente.getCodigo()                     : "");
        JTextField fNome      = campo(editando ? existente.getNome()                       : "");
        JTextField fPreco     = campo(editando ? String.valueOf(existente.getPreco())      : "");
        JTextField fEstoque   = campo(editando ? String.valueOf(existente.getEstoque())    : "");
        JTextField fCategoria = campo(editando ? existente.getCategoria()                  : "");

        form.add(rotulo("Código:"));    form.add(fCodigo);
        form.add(rotulo("Nome:"));      form.add(fNome);
        form.add(rotulo("Preço:"));     form.add(fPreco);
        form.add(rotulo("Estoque:"));   form.add(fEstoque);
        form.add(rotulo("Categoria:")); form.add(fCategoria);

        JButton btnSalvar   = TelaPrincipal.botaoPrimario(editando ? "Salvar Alterações" : "Cadastrar");
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dialog.dispose());

        btnSalvar.addActionListener(e -> {
            if (fNome.getText().isBlank() || fCodigo.getText().isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Código e Nome são obrigatórios.");
                return;
            }
            try {
                double preco   = Double.parseDouble(fPreco.getText().trim());
                int    estoque = Integer.parseInt(fEstoque.getText().trim());
                if (editando) {
                    existente.setNome(fNome.getText().trim());
                    existente.setPreco(preco);
                    existente.setEstoque(estoque);
                    existente.setCategoria(fCategoria.getText().trim());
                    sistema.salvarTodosProdutos();
                } else {
                    sistema.cadastrarProduto(new Produtos(
                        fCodigo.getText().trim(), fNome.getText().trim(),
                        preco, estoque, fCategoria.getText().trim()
                    ));
                }
                atualizarTabela();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Preço e Estoque devem ser números.");
            }
        });

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        rodape.setBackground(Color.WHITE);
        rodape.add(btnCancelar);
        rodape.add(btnSalvar);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(rodape, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void atualizarTabela() {
        modeloTabela.setRowCount(0);
        for (Produtos p : sistema.getProdutos()) {
            modeloTabela.addRow(new Object[]{
                p.getCodigo(), p.getNome(),
                String.format("R$ %.2f", p.getPreco()),
                p.getEstoque(), p.getCategoria()
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

    private JTextField campo(String valor) {
        JTextField f = new JTextField(valor);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225)),
            new EmptyBorder(6, 8, 6, 8)));
        return f;
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