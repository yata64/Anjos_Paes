package model;

import interfaces.Relatorio;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Vendas implements Relatorio, Serializable {

    private static final long serialVersionUID = 1L;
    public static int contadorVendas = 0;

    private int id;
    private Clientes cliente;
    private Funcionario funcionario;
    private ArrayList<Produtos> produtos;
    private LocalDate dataVenda;
    private String status;

    public Vendas(Clientes cliente, Funcionario funcionario) {
        this.id          = ++contadorVendas;
        this.cliente     = cliente;
        this.funcionario = funcionario;
        this.produtos    = new ArrayList<>();
        this.dataVenda   = LocalDate.now();
        this.status      = "CONCLUIDA";
    }

    public void adicionarProduto(Produtos produto) { produtos.add(produto); }
    public void removerProduto(Produtos produto)   { produtos.remove(produto); }

    public double calcularTotal() {
        double total = 0;
        for (Produtos produto : produtos) total += produto.getPreco();
        return total;
    }

    @Override
    public String gerarRelatorio() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("           RELATÓRIO DE VENDA           \n");
        sb.append("========================================\n");
        sb.append("ID Venda   : #").append(id).append("\n");
        sb.append("Data       : ").append(dataVenda.format(fmt)).append("\n");
        sb.append("Cliente    : ").append(cliente.getNome()).append("\n");
        sb.append("Vendedor   : ").append(funcionario.getNome()).append("\n");
        sb.append("Status     : ").append(status).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("PRODUTOS:\n");
        for (Produtos p : produtos) {
            sb.append(String.format("  %-25s R$ %.2f%n", p.getNome(), p.getPreco()));
        }
        sb.append("----------------------------------------\n");
        sb.append(String.format("TOTAL      : R$ %.2f%n", calcularTotal()));
        sb.append("========================================\n");
        return sb.toString();
    }

    /**
     * Formato CSV legível e recuperável:
     * id;cpfCliente;nomeCliente;matriculaFuncionario;nomeFuncionario;data;status;cod1,cod2
     *
     * CPF e matrícula → chaves para recarregar os objetos
     * nomeCliente e nomeFuncionario → legibilidade no arquivo
     */
    public String toCSV() {
        StringBuilder codigos = new StringBuilder();
        for (int i = 0; i < produtos.size(); i++) {
            if (i > 0) codigos.append(",");
            codigos.append(produtos.get(i).getCodigo());
        }
        return id                        + ";" +
               cliente.getCpf()         + ";" +
               cliente.getNome()        + ";" +
               funcionario.getMatricula() + ";" +
               funcionario.getNome()    + ";" +
               dataVenda               + ";" +
               status                  + ";" +
               codigos;
    }

    // Getters e Setters
    public int getId()                             { return id; }
    public void setId(int id)                      { this.id = id; }
    public Clientes getCliente()                   { return cliente; }
    public void setCliente(Clientes c)             { this.cliente = c; }
    public Funcionario getFuncionario()            { return funcionario; }
    public void setFuncionario(Funcionario f)      { this.funcionario = f; }
    public ArrayList<Produtos> getProdutos()       { return produtos; }
    public void setProdutos(ArrayList<Produtos> p) { this.produtos = p; }
    public LocalDate getDataVenda()                { return dataVenda; }
    public void setDataVenda(LocalDate d)          { this.dataVenda = d; }
    public String getStatus()                      { return status; }
    public void setStatus(String status)           { this.status = status; }

    @Override
    public String toString() {
        return "Venda #" + id + " | " + cliente.getNome() +
               " | R$ " + String.format("%.2f", calcularTotal());
    }
}