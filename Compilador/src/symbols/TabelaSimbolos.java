package symbols;

import java.util.ArrayList;
import java.util.List;

public class TabelaSimbolos {
    private final List<Escopo> todosEscopos = new ArrayList<Escopo>();
    private Escopo escopoAtual;
    private int proximoEndereco = 0;

    public TabelaSimbolos() {
        entrarEscopo("global", "global");
    }

    public void entrarEscopo(String nome, String categoria) {
        escopoAtual = new Escopo(nome, categoria, escopoAtual);
        todosEscopos.add(escopoAtual);
    }

    public void sairEscopo() {
        if (escopoAtual != null && escopoAtual.obterPai() != null) {
            escopoAtual = escopoAtual.obterPai();
        }
    }

    public boolean declarar(Simbolo simbolo) {
        if (escopoAtual == null) {
            return false;
        }
        int tamanho = simbolo.obterTipoDado() == null ? 4 : tamanhoSimbolo(simbolo.obterTipoDado());
        simbolo.definirEscopo(escopoAtual.obterNome());
        simbolo.definirEndereco(proximoEndereco);
        proximoEndereco += tamanho;
        return escopoAtual.declarar(simbolo);
    }

    public Simbolo resolver(String lexema) {
        return escopoAtual == null ? null : escopoAtual.resolver(lexema);
    }

    public String nomeEscopoAtual() {
        return escopoAtual == null ? "global" : escopoAtual.obterNome();
    }

    public String categoriaEscopoAtual() {
        return escopoAtual == null ? "global" : escopoAtual.obterCategoria();
    }

    public int tamanhoSimbolo(String tipo) {
        if (tipo == null) {
            return 4;
        }
        String base = tipo.replace("[]", "");
        if ("double".equals(base) || "long".equals(base)) {
            return 8;
        }
        if ("boolean".equals(base)) {
            return 1;
        }
        if ("char".equals(base)) {
            return 2;
        }
        if ("float".equals(base) || "int".equals(base)) {
            return 4;
        }
        return 4;
    }

    public void imprimir() {
        System.out.println("\nTABELA DE SIMBOLOS");
        System.out.println("============");
        for (Escopo escopo : todosEscopos) {
            System.out.println("Escopo: " + escopo.obterNome() + " (" + escopo.obterCategoria() + ")");
            for (Simbolo simbolo : escopo.obterSimbolos().values()) {
                System.out.println("  " + simbolo);
            }
        }
    }
}
