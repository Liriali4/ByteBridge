package lexer;


import java.util.ArrayList;
import java.util.List;

public class TabelaSimbolos {
    private List<Token> simbolos = new ArrayList<>();

    public void adicionar(Token t) {
        simbolos.add(t);
    }

    public void mostrar() {
        for (Token t : simbolos) {
            System.out.println(t);
        }
    }
}