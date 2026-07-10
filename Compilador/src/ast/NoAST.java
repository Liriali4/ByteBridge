package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * No generico da arvore sintatica abstracta (AST).
 *
 * Cada no guarda:
 *   - nome    -> a categoria/tipo do no (ex.: "classe", "binario", "identificador");
 *   - lexema  -> o texto relevante associado (nome da variavel, operador, valor do literal);
 *   - linha   -> a linha do codigo fonte onde o no comeca (usada nos erros semanticos);
 *   - tipo    -> preenchido durante a analise semantica com o tipo inferido;
 *   - filhos  -> sub-arvores.
 *
 * A arvore e construida pelo AnalisadorSintatico e percorrida pelo AnalisadorSemantico.
 * O parser NAO coloca aqui qualquer decisao semantica: limita-se a registar a estrutura.
 */
public class NoAST {
    private final String nome;
    private String lexema;
    private int linha;
    private String tipo;
    private final List<NoAST> filhos = new ArrayList<NoAST>();

    public NoAST(String nome) {
        this(nome, "", 0);
    }

    public NoAST(String nome, String lexema, int linha) {
        this.nome = nome;
        this.lexema = lexema;
        this.linha = linha;
    }

    public void adicionarFilho(NoAST filho) {
        if (filho != null) {
            filhos.add(filho);
        }
    }

    public String obterNome() {
        return nome;
    }

    public String obterLexema() {
        return lexema;
    }

    public void definirLexema(String lexema) {
        this.lexema = lexema;
    }

    public int obterLinha() {
        return linha;
    }

    public void definirLinha(int linha) {
        this.linha = linha;
    }

    public String obterTipo() {
        return tipo;
    }

    public void definirTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<NoAST> obterFilhos() {
        return Collections.unmodifiableList(filhos);
    }

    public NoAST obterFilho(int indice) {
        if (indice < 0 || indice >= filhos.size()) {
            return null;
        }
        return filhos.get(indice);
    }

    public int quantidadeFilhos() {
        return filhos.size();
    }

    @Override
    public String toString() {
        return nome + (lexema == null || lexema.isEmpty() ? "" : " '" + lexema + "'");
    }
}
