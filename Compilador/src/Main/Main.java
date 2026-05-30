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
    private static final String SEPARADOR = "==================================================";
    private static final String SUB_SEPARADOR = "--------------------------------------------------";

    public static void main(String[] args) {
        boolean mostrarTabela = contemOpcao(args, "--debug") || contemOpcao(args, "--tabela");
        String entrada = obterEntrada(args);
        Path caminhoArquivo = resolverArquivoEntrada(entrada);

        if (caminhoArquivo == null) {
            imprimirTitulo("ERRO AO ABRIR FICHEIRO");
            System.out.println();
            System.out.println("Ficheiro de entrada nao encontrado:");
            System.out.println(entrada);
            if (!ARQUIVO_PADRAO.equals(entrada)) {
                System.out.println("Caminho padrao esperado: " + ARQUIVO_PADRAO);
            }
            return;
        }

        imprimirTitulo("COMPILADOR - FASE 2: ANALISADOR SINTATICO");
        System.out.println();
        System.out.println("Arquivo: " + caminhoArquivo);
        System.out.println();
        System.out.println(SUB_SEPARADOR);
        System.out.println();

        try {
            AnalisadorLexico analisadorLexico = new AnalisadorLexico(caminhoArquivo.toString());
            AnalisadorSintatico analisadorSintatico = new AnalisadorSintatico(analisadorLexico);
            analisadorSintatico.analisarPrograma();

            if (analisadorSintatico.temErros()) {
                imprimirFalha(analisadorSintatico);
            } else {
                imprimirSucesso();
            }

            System.out.println();
            System.out.println("Total de erros: " + analisadorSintatico.obterErros().size());
            System.out.println();
            if (mostrarTabela) {
                analisadorSintatico.obterTabelaSimbolos().imprimir();
            } else {
                imprimirAviso("Tabela de simbolos nao exibida.");
                System.out.println("Use --debug ou --tabela para visualizar.");
            }
        } catch (RuntimeException e) {
            imprimirTitulo("COMPILACAO CONCLUIDA COM ERROS");
            System.out.println();
            System.out.println("Erro ao processar o ficheiro de entrada: " + caminhoArquivo);
            System.out.println("\nDetalhes: " + e.getMessage());
            System.out.println("\nTotal de erros: 1");
        }
    }

    private static void imprimirSucesso() {
        imprimirTitulo("COMPILACAO CONCLUIDA COM SUCESSO");
        System.out.println();
        System.out.println("Analise lexica concluida.");
        System.out.println("Analise sintatica concluida.");
        System.out.println("Nenhum erro encontrado.");
    }

    private static void imprimirFalha(AnalisadorSintatico analisadorSintatico) {
        int totalErros = analisadorSintatico.obterErros().size();

        imprimirTitulo("COMPILACAO CONCLUIDA COM ERROS");
        System.out.println();
        System.out.println("Foram encontrados " + totalErros + " erros.");
        System.out.println();
        System.out.println("ERROS ENCONTRADOS");
        System.out.println("-----------------");
        System.out.println();

        int numero = 1;
        for (ErroSintatico erro : analisadorSintatico.obterErros()) {
            System.out.println("[" + numero + "] " + erro);
            numero++;
        }
    }

    private static void imprimirTitulo(String titulo) {
        System.out.println(SEPARADOR);
        System.out.println(titulo);
        System.out.println(SEPARADOR);
    }

    private static void imprimirAviso(String mensagem) {
        System.out.println(mensagem);
    }

    private static boolean contemOpcao(String[] args, String opcao) {
        for (String arg : args) {
            if (opcao.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String obterEntrada(String[] args) {
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                return arg;
            }
        }
        return ARQUIVO_PADRAO;
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
