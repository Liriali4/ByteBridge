package lexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ANALISADOR LÉXICO - Implementação baseada em DFA
 * Compiladores - Primeira Fase
 * 
 * Autômato com 60+ estados para reconhecimento granular de tokens
 * Palavras reservadas reconhecidas diretamente no autômato
 * Utiliza tabela ASCII para classificação eficiente de caracteres
 * 
 * @author Engenharia de Compiladores
 * @version 2.0 - Refatoração Profissional
 */
public class AnalisadorLexico {
    
    // ========== CONSTANTES DE TOKENS (public static final) ==========
    public static final int TOKEN_IDENTIFICADOR = 1;
    public static final int TOKEN_IF = 2;
    public static final int TOKEN_WHILE = 3;
    public static final int TOKEN_INT = 4;
    public static final int TOKEN_FLOAT = 5;
    public static final int TOKEN_RETURN = 6;
    public static final int TOKEN_CLASS = 7;
    public static final int TOKEN_PUBLIC = 8;
    public static final int TOKEN_VOID = 9;
    public static final int TOKEN_NUMERO_INTEIRO = 10;
    public static final int TOKEN_NUMERO_REAL = 11;
    public static final int TOKEN_OP_ADICAO = 12;
    public static final int TOKEN_OP_SUBTRACAO = 13;
    public static final int TOKEN_OP_MULTIPLICACAO = 14;
    public static final int TOKEN_OP_DIVISAO = 15;
    public static final int TOKEN_OP_MODULO = 16;
    public static final int TOKEN_OP_MENOR = 17;
    public static final int TOKEN_OP_MAIOR = 18;
    public static final int TOKEN_OP_MENOR_IGUAL = 19;
    public static final int TOKEN_OP_MAIOR_IGUAL = 20;
    public static final int TOKEN_OP_IGUAL = 21;
    public static final int TOKEN_OP_DIFERENTE = 22;
    public static final int TOKEN_OP_ATRIBUICAO = 23;
    public static final int TOKEN_OP_AND = 24;
    public static final int TOKEN_OP_OR = 25;
    public static final int TOKEN_ABRE_PARENTESE = 26;
    public static final int TOKEN_FECHA_PARENTESE = 27;
    public static final int TOKEN_ABRE_CHAVE = 28;
    public static final int TOKEN_FECHA_CHAVE = 29;
    public static final int TOKEN_ABRE_COLCHETE = 30;
    public static final int TOKEN_FECHA_COLCHETE = 31;
    public static final int TOKEN_PONTO_VIRGULA = 32;
    public static final int TOKEN_VIRGULA = 33;
    public static final int TOKEN_PONTO = 34;
    public static final int TOKEN_STRING = 35;
    public static final int TOKEN_COMENTARIO = 36;
    public static final int TOKEN_FIM_ARQUIVO = 37;
    public static final int TOKEN_ERRO = 38;
    
    // ========== ATRIBUTOS ==========
    private char[] conteudo;
    private int pos = 0;
    private int linha = 1;
    private int coluna = 1;
    private TabelaSimbolos tabela;
    
    // Buffer para construção do lexema
    private StringBuilder lexema;
    
    /**
     * Construtor - Carrega o arquivo fonte
     */
    public AnalisadorLexico(String caminho) {
        try {
            String input = new String(Files.readAllBytes(Paths.get(caminho)));
            conteudo = input.toCharArray();
            tabela = new TabelaSimbolos();
            lexema = new StringBuilder();
        } catch (IOException e) {
            e.printStackTrace();
            conteudo = new char[0];
        }
    }
    
    /**
     * Inicia a análise léxica completa
     */
    public void iniciarAnalise() {
        Token token;
        do {
            token = analex();
            if (token.codigo != TOKEN_FIM_ARQUIVO) {
                gravarTokenLexema(token.lexema, token.codigo);
            }
        } while (token.codigo != TOKEN_FIM_ARQUIVO);
        
        tabela.mostrar();
    }
    
    /**
     * ANALEX - Analisador Léxico Principal
     * Implementa DFA 
     */
    public Token analex() {
        int estado = 0;
        lexema = new StringBuilder();
        char c;
        
        while (true) {
            switch (estado) {
                case 0: // Estado inicial
                    c = lerCaractere();
                    
                    if (c == '\0') {
                        return new Token("EOF", TOKEN_FIM_ARQUIVO);
                    }
                    
                    // Ignora whitespace (ASCII 9, 10, 13, 32)
                    if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                        continue;
                    }
                    
                    lexema.append(c);
                    
                    // Classificação por tabela ASCII
                    // Letras minúsculas: ASCII 97-122
                    if (c >= 'a' && c <= 'z') {
                        // Verifica início de palavras reservadas
                        if (c == 'i') estado = 10; // if ou int
                        else if (c == 'w') estado = 20; // while
                        else if (c == 'f') estado = 40; // float
                        else if (c == 'r') estado = 50; // return
                        else if (c == 'c') estado = 60; // class
                        else if (c == 'p') estado = 70; // public
                        else if (c == 'v') estado = 80; // void
                        else estado = 1; // identificador genérico
                    }
                    // Letras maiúsculas: ASCII 65-90
                    else if (c >= 'A' && c <= 'Z') {
                        estado = 1; // identificador
                    }
                    // Underscore: ASCII 95
                    else if (c == '_') {
                        estado = 1; // identificador
                    }
                    // Dígitos: ASCII 48-57
                    else if (c >= '0' && c <= '9') {
                        estado = 90; // número
                    }
                    // Operadores e símbolos
                    else if (c == '+') return new Token("+", TOKEN_OP_ADICAO);
                    else if (c == '-') return new Token("-", TOKEN_OP_SUBTRACAO);
                    else if (c == '*') return new Token("*", TOKEN_OP_MULTIPLICACAO);
                    else if (c == '%') return new Token("%", TOKEN_OP_MODULO);
                    else if (c == '/') estado = 100; // divisão ou comentário
                    else if (c == '<') estado = 101; // < ou <=
                    else if (c == '>') estado = 102; // > ou >=
                    else if (c == '=') estado = 103; // = ou ==
                    else if (c == '!') estado = 104; // !=
                    else if (c == '&') estado = 105; // &&
                    else if (c == '|') estado = 106; // ||
                    else if (c == '(') return new Token("(", TOKEN_ABRE_PARENTESE);
                    else if (c == ')') return new Token(")", TOKEN_FECHA_PARENTESE);
                    else if (c == '{') return new Token("{", TOKEN_ABRE_CHAVE);
                    else if (c == '}') return new Token("}", TOKEN_FECHA_CHAVE);
                    else if (c == '[') return new Token("[", TOKEN_ABRE_COLCHETE);
                    else if (c == ']') return new Token("]", TOKEN_FECHA_COLCHETE);
                    else if (c == ';') return new Token(";", TOKEN_PONTO_VIRGULA);
                    else if (c == ',') return new Token(",", TOKEN_VIRGULA);
                    else if (c == '.') return new Token(".", TOKEN_PONTO);
                    else if (c == '"') estado = 110; // string
                    else return new Token(String.valueOf(c), TOKEN_ERRO);
                    break;
                
                // ========== IDENTIFICADOR GENÉRICO ==========
                case 1:
                    c = lerCaractere();
                    // Letras (ASCII 65-90, 97-122), dígitos (48-57), underscore (95)
                    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                        (c >= '0' && c <= '9') || c == '_') {
                        lexema.append(c);
                        estado = 1;
     
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: if ==========
                case 10: // Leu 'i'
                    c = lerCaractere();
                    if (c == 'f') {
                        lexema.append(c);
                        estado = 11;
                    } else if (c == 'n') {
                        lexema.append(c);
                        estado = 30; // pode ser "int"
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1; // identificador
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 11: // Leu "if"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1; // identificador (ex: "iffy")
                    } else {
                        voltarCaractere();
                        return new Token("if", TOKEN_IF);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: while ==========
                case 20: // Leu 'w'
                    c = lerCaractere();
                    if (c == 'h') {
                        lexema.append(c);
                        estado = 21;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 21: // Leu "wh"
                    c = lerCaractere();
                    if (c == 'i') {
                        lexema.append(c);
                        estado = 22;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 22: // Leu "whi"
                    c = lerCaractere();
                    if (c == 'l') {
                        lexema.append(c);
                        estado = 23;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 23: // Leu "whil"
                    c = lerCaractere();
                    if (c == 'e') {
                        lexema.append(c);
                        estado = 24;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 24: // Leu "while"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("while", TOKEN_WHILE);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: int ==========
                case 30: // Leu "in"
                    c = lerCaractere();
                    if (c == 't') {
                        lexema.append(c);
                        estado = 31;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 31: // Leu "int"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("int", TOKEN_INT);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: float ==========
                case 40: // Leu 'f'
                    c = lerCaractere();
                    if (c == 'l') {
                        lexema.append(c);
                        estado = 41;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 41: // Leu "fl"
                    c = lerCaractere();
                    if (c == 'o') {
                        lexema.append(c);
                        estado = 42;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 42: // Leu "flo"
                    c = lerCaractere();
                    if (c == 'a') {
                        lexema.append(c);
                        estado = 43;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 43: // Leu "floa"
                    c = lerCaractere();
                    if (c == 't') {
                        lexema.append(c);
                        estado = 44;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 44: // Leu "float"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("float", TOKEN_FLOAT);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: return ==========
                case 50: // Leu 'r'
                    c = lerCaractere();
                    if (c == 'e') {
                        lexema.append(c);
                        estado = 51;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 51: // Leu "re"
                    c = lerCaractere();
                    if (c == 't') {
                        lexema.append(c);
                        estado = 52;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 52: // Leu "ret"
                    c = lerCaractere();
                    if (c == 'u') {
                        lexema.append(c);
                        estado = 53;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 53: // Leu "retu"
                    c = lerCaractere();
                    if (c == 'r') {
                        lexema.append(c);
                        estado = 54;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 54: // Leu "retur"
                    c = lerCaractere();
                    if (c == 'n') {
                        lexema.append(c);
                        estado = 55;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 55: // Leu "return"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("return", TOKEN_RETURN);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: class ==========
                case 60: // Leu 'c'
                    c = lerCaractere();
                    if (c == 'l') {
                        lexema.append(c);
                        estado = 61;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 61: // Leu "cl"
                    c = lerCaractere();
                    if (c == 'a') {
                        lexema.append(c);
                        estado = 62;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 62: // Leu "cla"
                    c = lerCaractere();
                    if (c == 's') {
                        lexema.append(c);
                        estado = 63;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 63: // Leu "clas"
                    c = lerCaractere();
                    if (c == 's') {
                        lexema.append(c);
                        estado = 64;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 64: // Leu "class"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("class", TOKEN_CLASS);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: public ==========
                case 70: // Leu 'p'
                    c = lerCaractere();
                    if (c == 'u') {
                        lexema.append(c);
                        estado = 71;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 71: // Leu "pu"
                    c = lerCaractere();
                    if (c == 'b') {
                        lexema.append(c);
                        estado = 72;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 72: // Leu "pub"
                    c = lerCaractere();
                    if (c == 'l') {
                        lexema.append(c);
                        estado = 73;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 73: // Leu "publ"
                    c = lerCaractere();
                    if (c == 'i') {
                        lexema.append(c);
                        estado = 74;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 74: // Leu "publi"
                    c = lerCaractere();
                    if (c == 'c') {
                        lexema.append(c);
                        estado = 75;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 75: // Leu "public"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("public", TOKEN_PUBLIC);
                    }
                    break;
                
                // ========== PALAVRA RESERVADA: void ==========
                case 80: // Leu 'v'
                    c = lerCaractere();
                    if (c == 'o') {
                        lexema.append(c);
                        estado = 81;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 81: // Leu "vo"
                    c = lerCaractere();
                    if (c == 'i') {
                        lexema.append(c);
                        estado = 82;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 82: // Leu "voi"
                    c = lerCaractere();
                    if (c == 'd') {
                        lexema.append(c);
                        estado = 83;
                    } else if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_IDENTIFICADOR);
                    }
                    break;
                
                case 83: // Leu "void"
                    c = lerCaractere();
                    if (ehLetraDigitoUnderscore(c)) {
                        lexema.append(c);
                        estado = 1;
                    } else {
                        voltarCaractere();
                        return new Token("void", TOKEN_VOID);
                    }
                    break;
                
                // ========== NÚMEROS ==========
                case 90: // Número inteiro
                    c = lerCaractere();
                    if (c >= '0' && c <= '9') { // ASCII 48-57
                        lexema.append(c);
                    } else if (c == '.') {
                        char proximo = peek();
                        if (proximo >= '0' && proximo <= '9') {
                            lexema.append(c);
                            estado = 91; // número real
                        } else {
                            voltarCaractere();
                            return new Token(lexema.toString(), TOKEN_NUMERO_INTEIRO);
                        }
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_NUMERO_INTEIRO);
                    }
                    break;
                
                case 91: // Após ponto decimal
                    c = lerCaractere();
                    if (c >= '0' && c <= '9') {
                        lexema.append(c);
                        estado = 92;
                    } else {
                        return new Token(lexema.toString(), TOKEN_ERRO);
                    }
                    break;
                
                case 92: // Número real
                    c = lerCaractere();
                    if (c >= '0' && c <= '9') {
                        lexema.append(c);
                    } else if (c == '.') {
                        lexema.append(c);
                        return new Token(lexema.toString(), TOKEN_ERRO); // segundo ponto
                    } else {
                        voltarCaractere();
                        return new Token(lexema.toString(), TOKEN_NUMERO_REAL);
                    }
                    break;
                
                // ========== OPERADORES ==========
                case 100: // Divisão ou comentário
                    c = lerCaractere();
                    if (c == '/') {
                        lexema = new StringBuilder();
                        estado = 107; // comentário de linha
                    } else if (c == '*') {
                        lexema = new StringBuilder();
                        estado = 108; // comentário de bloco
                    } else {
                        voltarCaractere();
                        return new Token("/", TOKEN_OP_DIVISAO);
                    }
                    break;
                
                case 101: // < ou <=
                    c = lerCaractere();
                    if (c == '=') {
                        return new Token("<=", TOKEN_OP_MENOR_IGUAL);
                    } else {
                        voltarCaractere();
                        return new Token("<", TOKEN_OP_MENOR);
                    }
                
                case 102: // > ou >=
                    c = lerCaractere();
                    if (c == '=') {
                        return new Token(">=", TOKEN_OP_MAIOR_IGUAL);
                    } else {
                        voltarCaractere();
                        return new Token(">", TOKEN_OP_MAIOR);
                    }
                
                case 103: // = ou ==
                    c = lerCaractere();
                    if (c == '=') {
                        return new Token("==", TOKEN_OP_IGUAL);
                    } else {
                        voltarCaractere();
                        return new Token("=", TOKEN_OP_ATRIBUICAO);
                    }
                
                case 104: // !=
                    c = lerCaractere();
                    if (c == '=') {
                        return new Token("!=", TOKEN_OP_DIFERENTE);
                    } else {
                        voltarCaractere();
                        return new Token("!", TOKEN_ERRO);
                    }
                
                case 105: // &&
                    c = lerCaractere();
                    if (c == '&') {
                        return new Token("&&", TOKEN_OP_AND);
                    } else {
                        voltarCaractere();
                        return new Token("&", TOKEN_ERRO);
                    }
                
                case 106: // ||
                    c = lerCaractere();
                    if (c == '|') {
                        return new Token("||", TOKEN_OP_OR);
                    } else {
                        voltarCaractere();
                        return new Token("|", TOKEN_ERRO);
                    }
                
                case 107: // Comentário de linha
                    c = lerCaractere();
                    if (c == '\0' || c == '\n') {
                        return new Token("//", TOKEN_COMENTARIO);
                    }
                    // Continua consumindo
                    break;
                
                case 108: // Comentário de bloco - início
                    c = lerCaractere();
                    if (c == '\0') {
                        return new Token("/*", TOKEN_ERRO); // não fechado
                    } else if (c == '*') {
                        estado = 109; // possível fim
                    }
                    // Continua consumindo
                    break;
                
                case 109: // Comentário de bloco - possível fim
                    c = lerCaractere();
                    if (c == '/') {
                        return new Token("/**/", TOKEN_COMENTARIO);
                    } else if (c == '*') {
                        // Permanece no estado 109
                    } else if (c == '\0') {
                        return new Token("/*", TOKEN_ERRO);
                    } else {
                        estado = 108; // volta para corpo do comentário
                    }
                    break;
                
                // ========== STRINGS ==========
                case 110: // String - início
                    c = lerCaractere();
                    if (c == '\0') {
                        return new Token(lexema.toString(), TOKEN_ERRO);
                    } else if (c == '"') {
                        lexema.append(c);
                        return new Token(lexema.toString(), TOKEN_STRING);
                    } else if (c == '\\') {
                        lexema.append(c);
                        estado = 111; // escape
                    } else if (c == '\n') {
                        return new Token(lexema.toString(), TOKEN_ERRO);
                    } else {
                        lexema.append(c);
                    }
                    break;
                
                case 111: // String - após escape
                    c = lerCaractere();
                    if (c == '\0') {
                        return new Token(lexema.toString(), TOKEN_ERRO);
                    } else {
                        lexema.append(c);
                        estado = 110; // volta para string
                    }
                    break;
                
                default:
                    return new Token("ERRO_INTERNO", TOKEN_ERRO);
            }
        }
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Lê o próximo caractere do buffer
     */
    private char lerCaractere() {
        if (pos >= conteudo.length) {
            return '\0';
        }
        char c = conteudo[pos++];
        if (c == '\n') {
            linha++;
            coluna = 1;
        } else {
            coluna++;
        }
        return c;
    }
    
    /**
     * Volta um caractere (lookahead/backtracking)
     */
    private void voltarCaractere() {
        if (pos > 0) {
            pos--;
            char c = conteudo[pos];
            if (c == '\n') {
                linha--;
                coluna = 1;
            } else {
                coluna--;
            }
        }
    }
    
    /**
     * Peek: olha o próximo caractere sem consumir
     */
    private char peek() {
        if (pos >= conteudo.length) {
            return '\0';
        }
        return conteudo[pos];
    }
    
    /**
     * Verifica se caractere é letra, dígito ou underscore
     * Usa tabela ASCII para eficiência
     */
    private boolean ehLetraDigitoUnderscore(char c) {
        return (c >= 'a' && c <= 'z') ||  // ASCII 97-122
               (c >= 'A' && c <= 'Z') ||  // ASCII 65-90
               (c >= '0' && c <= '9') ||  // ASCII 48-57
               (c == '_');                 // ASCII 95
    }
    
    /**
     * Grava token e lexema na tabela de símbolos
     */
    private void gravarTokenLexema(String lexema, int codigoToken) {
        Token token = new Token(lexema, codigoToken);
        tabela.adicionar(token);
    }
    
    /**
     * Retorna a tabela de símbolos
     */
    public TabelaSimbolos getTabelaSimbolos() {
        return tabela;
    }
    
    /**
     * Retorna nome do token pelo código
     */
    public static String getNomeToken(int codigo) {
        switch (codigo) {
            case TOKEN_IDENTIFICADOR: return "IDENTIFICADOR";
            case TOKEN_IF: return "IF";
            case TOKEN_WHILE: return "WHILE";
            case TOKEN_INT: return "INT";
            case TOKEN_FLOAT: return "FLOAT";
            case TOKEN_RETURN: return "RETURN";
            case TOKEN_CLASS: return "CLASS";
            case TOKEN_PUBLIC: return "PUBLIC";
            case TOKEN_VOID: return "VOID";
            case TOKEN_NUMERO_INTEIRO: return "NUMERO_INTEIRO";
            case TOKEN_NUMERO_REAL: return "NUMERO_REAL";
            case TOKEN_OP_ADICAO: return "OP_ADICAO";
            case TOKEN_OP_SUBTRACAO: return "OP_SUBTRACAO";
            case TOKEN_OP_MULTIPLICACAO: return "OP_MULTIPLICACAO";
            case TOKEN_OP_DIVISAO: return "OP_DIVISAO";
            case TOKEN_OP_MODULO: return "OP_MODULO";
            case TOKEN_OP_MENOR: return "OP_MENOR";
            case TOKEN_OP_MAIOR: return "OP_MAIOR";
            case TOKEN_OP_MENOR_IGUAL: return "OP_MENOR_IGUAL";
            case TOKEN_OP_MAIOR_IGUAL: return "OP_MAIOR_IGUAL";
            case TOKEN_OP_IGUAL: return "OP_IGUAL";
            case TOKEN_OP_DIFERENTE: return "OP_DIFERENTE";
            case TOKEN_OP_ATRIBUICAO: return "OP_ATRIBUICAO";
            case TOKEN_OP_AND: return "OP_AND";
            case TOKEN_OP_OR: return "OP_OR";
            case TOKEN_ABRE_PARENTESE: return "ABRE_PARENTESE";
            case TOKEN_FECHA_PARENTESE: return "FECHA_PARENTESE";
            case TOKEN_ABRE_CHAVE: return "ABRE_CHAVE";
            case TOKEN_FECHA_CHAVE: return "FECHA_CHAVE";
            case TOKEN_ABRE_COLCHETE: return "ABRE_COLCHETE";
            case TOKEN_FECHA_COLCHETE: return "FECHA_COLCHETE";
            case TOKEN_PONTO_VIRGULA: return "PONTO_VIRGULA";
            case TOKEN_VIRGULA: return "VIRGULA";
            case TOKEN_PONTO: return "PONTO";
            case TOKEN_STRING: return "STRING";
            case TOKEN_COMENTARIO: return "COMENTARIO";
            case TOKEN_FIM_ARQUIVO: return "FIM_ARQUIVO";
            case TOKEN_ERRO: return "ERRO";
            default: return "DESCONHECIDO";
        }
    }
}
