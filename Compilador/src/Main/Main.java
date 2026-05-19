package Main;

import lexer.AnalisadorLexico;

/**
 * Classe principal para teste do Analisador Léxico
 * 
 * Uso: java Main.Main [arquivo]
 * Se nenhum arquivo for especificado, usa "codigo.txt" por padrão
 */
public class Main {

    public static void main(String[] args) {
        // Caminho do arquivo de entrada
        String arquivo = args.length > 0 ? args[0] : "E:\\3ANO\\COMPILADORES\\Trabalho_Pratico\\Compilador\\teste_comentarios.txt";
        
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   ANALISADOR LÉXICO                  ║");
        System.out.println("║   Compiladores - Primeira Fase                 ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("\nArquivo: " + arquivo);
        System.out.println("─────────────────────────────────────────────────\n");
        
        AnalisadorLexico analisador = new AnalisadorLexico(arquivo);
        analisador.iniciarAnalise();
        
        System.out.println("\n─────────────────────────────────────────────────");
        System.out.println("✓ Análise léxica concluída com sucesso!");
        System.out.println("─────────────────────────────────────────────────");
    }

}
