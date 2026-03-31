package Main;

import lexer.AnalisadorLexico;

public class Main {

    public static void main(String[] args) {
        AnalisadorLexico analisador = new AnalisadorLexico(
            "E:\\3ANO\\COMPILADORES\\Trabalho_Pratico\\Compilador\\codigo.txt"
        );

        analisador.analex();

    }

}
