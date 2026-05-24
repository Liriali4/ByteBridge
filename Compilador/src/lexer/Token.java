package lexer;

/**
 * Classe Token - representa um token reconhecido pelo analisador lexico.
 */
public class Token {
    public String lexema;
    public int codigo;
    public int linha;
    
    public Token(String lexema, int codigo) {
        this(lexema, codigo, -1);
    }

    public Token(String lexema, int codigo, int linha) {
        this.lexema = lexema;
        this.codigo = codigo;
        this.linha = linha;
    }
    
    @Override
    public String toString() {
        return AnalisadorLexico.getNomeToken(codigo) + ": " + lexema;
    }
}
