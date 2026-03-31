package lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import utils.TipoToken;

public class AnalisadorLexico {

    private String conteudo;
    private int pos = 0;
    private TabelaSimbolos tabela = new TabelaSimbolos();

    // Palavras reservadas
    private String[] reservadas = {"if", "while", "int", "float", "return"};

    public AnalisadorLexico(String caminho) {
        try {
            conteudo = new String(Files.readAllBytes(Paths.get(caminho)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===============================
    // FUNÇÃO PRINCIPAL (AUTÓMATO)
    // ===============================
    public void analex() {

        int estado = 0;
        String lexema = "";
        char c;

        while ((c = lerCaractere()) != '\0') {

            switch (estado) {

                case 0:
                    if (Character.isLetter(c)) {
                        estado = 1;
                        lexema += c;
                    } else if (Character.isDigit(c)) {
                        estado = 2;
                        lexema += c;
                    } else if (c == '<' || c == '>' || c == '=') {
                        estado = 3;
                        lexema += c;
                    } else if ("+-*/".indexOf(c) != -1) {
                        gravarTokenLexema(String.valueOf(c), TipoToken.OPERADOR);
                    } else if ("(){};,".indexOf(c) != -1) {
                        gravarTokenLexema(String.valueOf(c), TipoToken.SIMBOLO);
                    }
                    break;

                // IDENTIFICADOR ou RESERVADA
                case 1:
                    if (Character.isLetterOrDigit(c)) {
                        lexema += c;
                    } else {
                        voltarCaractere();
                        if (ehReservada(lexema)) {
                            gravarTokenLexema(lexema, TipoToken.RESERVADA);
                        } else {
                            gravarTokenLexema(lexema, TipoToken.IDENTIFICADOR);
                        }
                        lexema = "";
                        estado = 0;
                    }
                    break;

                // NÚMERO
                case 2:
                    if (Character.isDigit(c)) {
                        lexema += c;
                    } else {
                        voltarCaractere();
                        gravarTokenLexema(lexema, TipoToken.NUMERO);
                        lexema = "";
                        estado = 0;
                    }
                    break;

                // OPERADORES RELACIONAIS
                case 3:
                    if (c == '=') {
                        lexema += c;
                        gravarTokenLexema(lexema, TipoToken.RELACIONAL);
                    } else {
                        voltarCaractere();
                        gravarTokenLexema(lexema, TipoToken.RELACIONAL);
                    }
                    lexema = "";
                    estado = 0;
                    break;
            }
        }

        tabela.mostrar();
    }

    // ===============================
    // FUNÇÕES EXIGIDAS NO ENUNCIADO
    // ===============================
    public char lerCaractere() {
        if (pos >= conteudo.length()) {
            return '\0';
        }
        return conteudo.charAt(pos++);
    }

    public void voltarCaractere() {
        pos--;
    }

    public void gravarTokenLexema(String lexema, String tipo) {
        tabela.adicionar(new Token(lexema, tipo));
    }

    // ===============================
    // AUXILIAR
    // ===============================
    private boolean ehReservada(String lexema) {
        for (String r : reservadas) {
            if (r.equals(lexema)) {
                return true;
            }
        }
        return false;
    }
}
