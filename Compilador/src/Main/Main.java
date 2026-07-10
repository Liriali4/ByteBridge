package Main;

import ast.NoAST;
import errors.ErroSemantico;
import errors.ErroSintatico;
import lexer.AnalisadorLexico;
import parser.AnalisadorSintatico;
import semantic.AnalisadorSemantico;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Ponto de entrada do compilador.
 *
 * Executa a cadeia completa das tres fases, cada uma com responsabilidades
 * bem definidas:
 *
 *   AnalisadorLexico  -> transforma o texto em tokens;
 *   AnalisadorSintatico -> verifica a gramatica, constroi a tabela de simbolos
 *                          e a arvore sintatica (AST);
 *   AnalisadorSemantico -> percorre a AST e a tabela e verifica as regras
 *                          semanticas (tipos, declaracoes, chamadas, condicoes).
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

        imprimirTitulo("COMPILADOR - FASES 1 A 3");
        System.out.println();
        System.out.println("Arquivo: " + caminhoArquivo);
        System.out.println();
        System.out.println(SUB_SEPARADOR);
        System.out.println();

        try {
            // Fase 1 + Fase 2: analise lexica e sintatica (constroi tabela e AST).
            // A entrada e normalizada para terminar sempre com uma mudanca de linha:
            // o analisador lexico so devolve o token de fim de ficheiro depois de um
            // separador apos o ultimo lexema. Ficheiros sem '\n' final sao tratados
            // aqui, no arranque, sem alterar o analisador lexico.
            Path caminhoNormalizado = normalizarEntrada(caminhoArquivo);
            AnalisadorLexico analisadorLexico = new AnalisadorLexico(caminhoNormalizado.toString());
            AnalisadorSintatico analisadorSintatico = new AnalisadorSintatico(analisadorLexico);
            NoAST arvore = analisadorSintatico.analisarPrograma();

            // Fase 3: analise semantica sobre a AST e a tabela de simbolos.
            AnalisadorSemantico analisadorSemantico =
                    new AnalisadorSemantico(arvore, analisadorSintatico.obterTabelaSimbolos());
            List<ErroSemantico> errosSemanticos = analisadorSemantico.analisar();

            List<ErroRelatorio> erros = combinarErros(analisadorSintatico.obterErros(), errosSemanticos);
            imprimirRelatorioErros(erros);

            int totalErros = erros.size();
            System.out.println();
            if (totalErros == 0) {
                imprimirSucesso();
            } else {
                System.out.println("---");
                System.out.println();
                System.out.println("Compilacao terminada com " + totalErros + " erro(s).");
            }
            System.out.println();
            System.out.println("Total de erros: " + totalErros);
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
        System.out.println("Analise semantica concluida.");
        System.out.println("Nenhum erro encontrado.");
    }

    private static void imprimirRelatorioErros(List<ErroRelatorio> erros) {
        System.out.println("ERROS ENCONTRADOS");
        System.out.println();
        if (erros.isEmpty()) {
            System.out.println("Nenhum erro encontrado.");
        } else {
            int numero = 1;
            for (ErroRelatorio erro : erros) {
                System.out.println("[" + numero + "] Linha " + erro.linha + ": " + erro.mensagem);
                numero++;
            }
        }
    }

    private static List<ErroRelatorio> combinarErros(List<ErroSintatico> errosSintaticos, List<ErroSemantico> errosSemanticos) {
        List<ErroRelatorio> erros = new ArrayList<ErroRelatorio>();
        int ordem = 0;
        for (ErroSintatico erro : errosSintaticos) {
            erros.add(new ErroRelatorio(erro.obterLinha(), limparMensagem(erro.toString(), erro.obterLinha()), ordem++));
        }
        for (ErroSemantico erro : errosSemanticos) {
            erros.add(new ErroRelatorio(erro.obterLinha(), limparMensagem(erro.toString(), erro.obterLinha()), ordem++));
        }
        Collections.sort(erros, new Comparator<ErroRelatorio>() {
            @Override
            public int compare(ErroRelatorio a, ErroRelatorio b) {
                if (a.linha != b.linha) {
                    return a.linha - b.linha;
                }
                return a.ordem - b.ordem;
            }
        });
        return erros;
    }

    private static String limparMensagem(String mensagem, int linha) {
        String prefixo = "Erro na linha " + linha;
        if (mensagem.startsWith(prefixo)) {
            mensagem = mensagem.substring(prefixo.length()).trim();
        }
        if (mensagem.startsWith(":")) {
            mensagem = mensagem.substring(1).trim();
        }
        return mensagem;
    }

    private static class ErroRelatorio {
        private final int linha;
        private final String mensagem;
        private final int ordem;

        private ErroRelatorio(int linha, String mensagem, int ordem) {
            this.linha = linha;
            this.mensagem = mensagem;
            this.ordem = ordem;
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

    /**
     * Garante que o ficheiro entregue ao analisador lexico termina com uma
     * mudanca de linha. Se ja terminar, devolve o proprio caminho; caso
     * contrario, cria uma copia temporaria com um '\n' final e devolve-a.
     * Esta normalizacao evita que o analisador lexico fique preso no ultimo
     * lexema quando o ficheiro nao tem separador final.
     */
    private static Path normalizarEntrada(Path caminho) {
        try {
            byte[] dados = Files.readAllBytes(caminho);
            if (dados.length > 0) {
                char ultimo = (char) dados[dados.length - 1];
                if (ultimo == '\n' || ultimo == '\r') {
                    return caminho;
                }
            }
            Path temporario = Files.createTempFile("compilador_entrada_", ".txt");
            temporario.toFile().deleteOnExit();
            byte[] normalizado = new byte[dados.length + 1];
            System.arraycopy(dados, 0, normalizado, 0, dados.length);
            normalizado[dados.length] = (byte) '\n';
            Files.write(temporario, normalizado);
            return temporario;
        } catch (java.io.IOException e) {
            return caminho;
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
