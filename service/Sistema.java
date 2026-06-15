package service;

import exceptions.AutenticacaoException;
import exceptions.EstoqueInsuficienteException;
import exceptions.VendaFinalizadaException;
import model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Sistema {

    private List<Clientes>    clientes     = new ArrayList<>();
    private List<Funcionario> funcionarios = new ArrayList<>();
    private List<Produtos>    produtos     = new ArrayList<>();
    private List<Vendas>      vendas       = new ArrayList<>();

    private Autenticacao autenticador;

    private static final String ARQ_CLIENTES     = "clientes.csv";
    private static final String ARQ_FUNCIONARIOS = "funcionarios.csv";
    private static final String ARQ_PRODUTOS     = "produtos.csv";
    private static final String ARQ_VENDAS       = "vendas.csv";

    // ══════════════════════════ AUTENTICAÇÃO ══════════════════════════════════

    public Funcionario login(String matricula, String senha) throws AutenticacaoException {
        if (autenticador == null) autenticador = new Autenticacao(funcionarios);
        return autenticador.login(matricula, senha);
    }

    public void logout() { if (autenticador != null) autenticador.logout(); }

    public Funcionario getFuncionarioLogado() {
        return autenticador != null ? autenticador.getFuncionarioLogado() : null;
    }

    public boolean estaLogado() { return autenticador != null && autenticador.estaLogado(); }
    public boolean isAdmin()    { return autenticador != null && autenticador.isAdmin(); }

    // ══════════════════════════ CLIENTES ══════════════════════════════════════

    public void cadastrarCliente(Clientes cliente) {
        clientes.add(cliente);
        PersistenciaService.salvarDados(ARQ_CLIENTES, cliente.toCSV());
    }

    public Clientes buscarClientePorCpf(String cpf) {
        for (Clientes c : clientes)
            if (c.getCpf().equals(cpf)) return c;
        return null;
    }

    public Clientes buscarClientePorNome(String nome) {
        for (Clientes c : clientes)
            if (c.getNome().equalsIgnoreCase(nome)) return c;
        return null;
    }

    public List<Clientes> getClientes() { return clientes; }

    // ══════════════════════════ FUNCIONÁRIOS ══════════════════════════════════

    public void cadastrarFuncionario(Funcionario funcionario) {
        funcionarios.add(funcionario);
        PersistenciaService.salvarDados(ARQ_FUNCIONARIOS, funcionario.toCSV());
        if (autenticador == null) autenticador = new Autenticacao(funcionarios);
    }

    public Funcionario buscarFuncionarioPorMatricula(String matricula) {
        for (Funcionario f : funcionarios)
            if (f.getMatricula().equals(matricula)) return f;
        return null;
    }

    public List<Funcionario> getFuncionarios() { return funcionarios; }

    // ══════════════════════════ PRODUTOS ══════════════════════════════════════

    public void cadastrarProduto(Produtos produto) {
        produtos.add(produto);
        PersistenciaService.salvarDados(ARQ_PRODUTOS, produto.toCSV());
    }

    public Produtos buscarProdutoPorCodigo(String codigo) {
        for (Produtos p : produtos)
            if (p.getCodigo().equals(codigo)) return p;
        return null;
    }

    public void reduzirEstoque(Produtos produto, int quantidade)
            throws EstoqueInsuficienteException {
        if (!produto.temEstoque(quantidade))
            throw new EstoqueInsuficienteException(
                    produto.getNome(), produto.getEstoque(), quantidade);
        produto.reduzirEstoque(quantidade);
    }

    public List<Produtos> getProdutos() { return produtos; }

    // ══════════════════════════ VENDAS ════════════════════════════════════════

    public void registrarVenda(Vendas venda) throws VendaFinalizadaException {
        for (Vendas v : vendas)
            if (v.getId() == venda.getId())
                throw new VendaFinalizadaException(venda.getId());
        vendas.add(venda);
        PersistenciaService.salvarDados(ARQ_VENDAS, venda.toCSV());
    }

    public List<Vendas> getVendas() { return vendas; }

    // ══════════════════════════ RELATÓRIOS ════════════════════════════════════

    public void gerarRelatorioVendas() {
        if (vendas.isEmpty()) { System.out.println("Nenhuma venda registrada."); return; }
        double totalGeral = 0;
        for (Vendas v : vendas) {
            System.out.println(v.gerarRelatorio());
            totalGeral += v.calcularTotal();
        }
        System.out.printf("TOTAL GERAL: R$ %.2f%n", totalGeral);
    }

    // ══════════════════════════ PERSISTÊNCIA ══════════════════════════════════

    public void carregarDados() {
        // Ordem importa: produtos → clientes → funcionários → vendas
        for (String l : PersistenciaService.carregarDados(ARQ_PRODUTOS))
            try { produtos.add(Produtos.fromCSV(l)); } catch (Exception ignored) {}

        for (String l : PersistenciaService.carregarDados(ARQ_CLIENTES))
            try { clientes.add(Clientes.fromCSV(l)); } catch (Exception ignored) {}

        for (String l : PersistenciaService.carregarDados(ARQ_FUNCIONARIOS))
            try { funcionarios.add(Funcionario.fromCSV(l)); } catch (Exception ignored) {}

        // Vendas: usa CPF e matrícula para encontrar os objetos reais
        // Formato: id;cpfCliente;nomeCliente;matriculaFunc;nomeFunc;data;status;cod1,cod2
        for (String l : PersistenciaService.carregarDados(ARQ_VENDAS)) {
            try {
                String[] c = l.split(";");
                if (c.length < 7) continue;

                int         id          = Integer.parseInt(c[0]);
                String      cpfCliente  = c[1];
                // c[2] = nomeCliente  → só para leitura humana, não usado aqui
                String      matricula   = c[3];
                // c[4] = nomeFuncionario → só para leitura humana, não usado aqui
                LocalDate   data        = LocalDate.parse(c[5]);
                String      status      = c[6];
                String[]    codigos     = c.length > 7 && !c[7].isBlank()
                                            ? c[7].split(",")
                                            : new String[0];

                // Busca os objetos reais pelo CPF e matrícula
                Clientes    cliente     = buscarClientePorCpf(cpfCliente);
                Funcionario funcionario = buscarFuncionarioPorMatricula(matricula);

                if (cliente == null || funcionario == null) continue;

                Vendas venda = new Vendas(cliente, funcionario);
                venda.setId(id);
                venda.setDataVenda(data);
                venda.setStatus(status);

                for (String cod : codigos) {
                    Produtos p = buscarProdutoPorCodigo(cod.trim());
                    if (p != null) venda.adicionarProduto(p);
                }

                vendas.add(venda);
                if (id >= Vendas.contadorVendas) Vendas.contadorVendas = id;

            } catch (Exception ignored) {}
        }

        autenticador = new Autenticacao(funcionarios);

        System.out.println("[Sistema] Carregados: " +
            produtos.size()     + " produto(s), " +
            clientes.size()     + " cliente(s), " +
            funcionarios.size() + " funcionário(s), " +
            vendas.size()       + " venda(s).");
    }
}