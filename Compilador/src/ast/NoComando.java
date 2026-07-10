package ast;

/**
 * No que representa um comando (statement): declaracao local, atribuicao,
 * if, while, for, return, break, continue ou um bloco.
 * Existe para tornar a arvore mais legivel e permitir distinguir, durante
 * a analise semantica, comandos de expressoes.
 */
public class NoComando extends NoAST {
    public NoComando(String nome) {
        super(nome);
    }

    public NoComando(String nome, String lexema, int linha) {
        super(nome, lexema, linha);
    }
}
