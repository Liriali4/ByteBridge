package lexer;

/**
 * Classe Token - Representa um token reconhecido pelo analisador léxico
 */
public class Token {
    public String lexema;
    public int codigo;
    
    public Token(String lexema, int codigo) {
        this.lexema = lexema;
        this.codigo = codigo;
    }
    
    @Override
    public String toString() {
        return AnalisadorLexico.getNomeToken(codigo) + ": " + lexema;
    }
}
