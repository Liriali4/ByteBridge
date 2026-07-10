package ast;

/**
 * No que representa uma expressao: literais, identificadores, operacoes
 * binarias/unarias, atribuicoes, chamadas de metodo, acessos a arrays, etc.
 * O campo "tipo" (herdado de NoAST) e preenchido pelo AnalisadorSemantico
 * com o tipo inferido da expressao.
 */
public class NoExpressao extends NoAST {
    public NoExpressao(String nome) {
        super(nome);
    }

    public NoExpressao(String nome, String lexema, int linha) {
        super(nome, lexema, linha);
    }
}
