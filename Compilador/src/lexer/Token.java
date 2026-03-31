package lexer;

public class Token {
    public String lexema;
    public String tipo;

    public Token(String lexema, String tipo) {
        this.lexema = lexema;
        this.tipo = tipo;
    }
}