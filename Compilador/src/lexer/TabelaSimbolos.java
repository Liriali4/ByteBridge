package lexer;

import java.util.ArrayList;

public class TabelaSimbolos {
    private ArrayList<Token> tabela = new ArrayList<>();

    public void adicionar(Token t) {
        tabela.add(t);
    }

    public void mostrar() {
        System.out.println("\n--- TABELA DE SÍMBOLOS ---");
        for (Token t : tabela) {
            System.out.println(t.lexema + " -> " + t.tipo);
        }
    }
}