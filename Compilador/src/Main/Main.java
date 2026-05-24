package Main;

import errors.ErroSintatico;
import lexer.AnalisadorLexico;
import parser.AnalisadorSintatico;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ponto de entrada da Fase 2: analise lexica seguida de analise sintatica
 * descendente recursiva e construcao da tabela de simbolos.
 */
public class Main {
    private static final String ARQUIVO_PADRAO = "testes/teste_parser_erros.java";

    public static void main(String[] args) {
        String entrada = args.length > 0 ? args[0] : ARQUIVO_PADRAO;
        Path caminhoArquivo = resolverArquivoEntrada(entrada);

        if (caminhoArquivo == null) {
            System.out.println("Erro: ficheiro de entrada nao encontrado:");
            System.out.println(entrada);
            if (!ARQUIVO_PADRAO.equals(entrada)) {
                System.out.println("Caminho padrao esperado: " + ARQUIVO_PADRAO);
            }
            return;
        }

        System.out.println("COMPILADOR - FASE 2: ANALISADOR SINTATICO");
        System.out.println("Arquivo: " + caminhoArquivo);
        System.out.println("--------------------------------------------------");

        try {
            AnalisadorLexico analisadorLexico = new AnalisadorLexico(caminhoArquivo.toString());
            AnalisadorSintatico analisadorSintatico = new AnalisadorSintatico(analisadorLexico);
            analisadorSintatico.analisarPrograma();

            if (analisadorSintatico.temErros()) {
                System.out.println("Foram encontrados erros sintaticos/semanticos:");
                for (ErroSintatico erro : analisadorSintatico.obterErros()) {
                    System.out.println(erro);
                }
            } else {
                System.out.println("Analise sintatica concluida com sucesso.");
            }

            analisadorSintatico.obterTabelaSimbolos().imprimir();
        } catch (RuntimeException e) {
            System.out.println("Erro ao processar o ficheiro de entrada: " + caminhoArquivo);
            System.out.println("Detalhes: " + e.getMessage());
        }
    }

    private static Path resolverArquivoEntrada(String entrada) {
        Path informado = Paths.get(entrada);
        if (Files.exists(informado)) {
            return informado;
        }

        Path[] candidatos = new Path[] {
            Paths.get(entrada),
            Paths.get("Compilador").resolve(entrada),
            Paths.get("..").resolve(entrada)
        };

        for (Path candidato : candidatos) {
            if (Files.exists(candidato)) {
                return candidato.normalize();
            }
        }

        return null;
    }
}
