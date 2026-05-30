package symbols;

import java.util.ArrayList;
import java.util.List;

public class Simbolo {
    private final String token;
    private final String lexema;
    private final int linha;
    private final String categoria;
    private String tipoDado;
    private String tipoVariavel;
    private String escopo;
    private String valor;
    private int endereco;
    private int tamanho;
    private boolean inicializado;
    private int dimensoes;
    private final List<String> parametros = new ArrayList<String>();
    private final List<String> modificadores = new ArrayList<String>();
    private String tipoRetorno;

    public Simbolo(String token, String lexema, int linha, String categoria) {
        this.token = token;
        this.lexema = lexema;
        this.linha = linha;
        this.categoria = categoria;
        this.valor = "";
    }

    public String obterToken() {
        return token;
    }

    public String obterLexema() {
        return lexema;
    }

    public int obterLinha() {
        return linha;
    }

    public String obterCategoria() {
        return categoria;
    }

    public int obterTamanho() {
        return tamanho;
    }

    public String obterTipoDado() {
        return tipoDado;
    }

    public void definirTipoDado(String tipoDado) {
        this.tipoDado = tipoDado;
    }

    public void definirTipoVariavel(String tipoVariavel) {
        this.tipoVariavel = tipoVariavel;
    }

    public void definirEscopo(String escopo) {
        this.escopo = escopo;
    }

    public void definirValor(String valor) {
        this.valor = valor;
    }

    public void definirEndereco(int endereco) {
        this.endereco = endereco;
    }

    public void definirTamanho(int tamanho) {
        this.tamanho = tamanho;
    }

    public void definirInicializado(boolean inicializado) {
        this.inicializado = inicializado;
    }

    public void definirDimensoes(int dimensoes) {
        this.dimensoes = dimensoes;
    }

    public List<String> obterParametros() {
        return parametros;
    }

    public List<String> obterModificadores() {
        return modificadores;
    }

    public void definirTipoRetorno(String tipoRetorno) {
        this.tipoRetorno = tipoRetorno;
    }

    @Override
    public String toString() {
        return String.format("%-14s %-16s linha=%-3d categoria=%-10s tipoDado=%-12s tipoVariavel=%-10s escopo=%-16s valor=%-10s endereco=%-4d tamanho=%-3d inicializado=%-5s dimensoes=%-2d parametros=%s modificadores=%s tipoRetorno=%s",
                token, lexema, linha, categoria, nuloParaVazio(tipoDado), nuloParaVazio(tipoVariavel),
                nuloParaVazio(escopo), nuloParaVazio(valor), endereco, tamanho, inicializado, dimensoes,
                parametros, modificadores, nuloParaVazio(tipoRetorno));
    }

    private String nuloParaVazio(String valor) {
        return valor == null ? "" : valor;
    }
}
