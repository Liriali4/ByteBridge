package parser;

import ast.NoAST;
import ast.NoComando;
import ast.NoExpressao;
import errors.ErroSintatico;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lexer.AnalisadorLexico;
import lexer.Token;
import symbols.Simbolo;
import symbols.TabelaSimbolos;

/**
 * Analisador sintatico descendente recursivo (Fase 2).
 *
 * Responsabilidades (e apenas estas):
 *   1. Verificar que a sequencia de tokens respeita a gramatica (java_grammar.txt);
 *   2. Construir a Tabela de Simbolos com as declaracoes encontradas;
 *   3. Construir a Arvore Sintatica Abstracta (AST) que sera percorrida pela Fase 3.
 *
 * O parser NAO faz analise semantica: nao verifica tipos, nem uso de variaveis
 * nao declaradas, nem redeclaracoes. Essas verificacoes pertencem ao
 * AnalisadorSemantico e usam a tabela e a AST aqui produzidas.
 *
 * Recuperacao de erros (modo panico):
 *   - o erro e SEMPRE registado antes de iniciar qualquer sincronizacao, para
 *     que a linha apresentada seja a do ponto onde o erro realmente ocorreu;
 *   - para simbolos terminadores em falta (";", ")", "]", "}") a linha usada e
 *     a do ultimo token valido consumido, e nao a do token onde a analise parou;
 *   - existe uma sincronizacao especifica para cada contexto (classe, metodo,
 *     declaracao, bloco, comando, expressao, parametros);
 *   - todos os ciclos de analise garantem progresso, evitando ciclos infinitos.
 */
public class AnalisadorSintatico {
    private static final Set<String> PALAVRAS_RESERVADAS = new HashSet<String>(Arrays.asList(
            "package", "import", "class", "extends", "public", "private", "protected", "static", "final",
            "void", "if", "else", "while", "for", "return", "break", "continue", "new", "this", "super",
            "true", "false", "null", "int", "double", "boolean", "char", "float", "long", "String"));

    // Conjuntos de sincronizacao, um por contexto de recuperacao de erros.
    private static final Set<String> SINCRONIZACAO_CLASSE = new HashSet<String>(Arrays.asList(
            "class"));
    private static final Set<String> SINCRONIZACAO_MEMBRO = new HashSet<String>(Arrays.asList(
            "}", "public", "private", "protected", "static", "final", "void",
            "int", "double", "boolean", "char", "float", "long", "String"));
    private static final Set<String> SINCRONIZACAO_DECLARACAO = new HashSet<String>(Arrays.asList(
            ";", "}", "class", "public", "private", "protected", "static", "final", "void",
            "int", "double", "boolean", "char", "float", "long", "String"));
    private static final Set<String> SINCRONIZACAO_BLOCO = new HashSet<String>(Arrays.asList(
            "}", ";", "{", "if", "while", "for", "return", "break", "continue",
            "int", "double", "boolean", "char", "float", "long", "String"));
    private static final Set<String> SINCRONIZACAO_EXPRESSAO = new HashSet<String>(Arrays.asList(
            ";", ")", "]", ",", "}", ":"));
    private static final Set<String> SINCRONIZACAO_PARAMETROS = new HashSet<String>(Arrays.asList(
            ")", ",", "{"));
    private static final Set<String> SINCRONIZACAO_ESTRUTURA = new HashSet<String>(Arrays.asList(
            ")", "{", "}", ";", "else"));
    // Simbolos terminadores: quando faltam, o erro aponta ao fim da construcao.
    private static final Set<String> TERMINADORES = new HashSet<String>(Arrays.asList(
            ";", ")", "]", "}", ":"));

    private final AnalisadorLexico lexer;
    private final TabelaSimbolos tabelaSimbolos = new TabelaSimbolos();
    private final List<ErroSintatico> listaErros = new ArrayList<ErroSintatico>();
    private Token tokenAtual;
    private Token tokenSeguinte;
    private Token ultimoTokenConsumido;
    private int contadorBlocos = 0;
    private int totalAvancos = 0;
    private String ultimoErro = "";

    public AnalisadorSintatico(AnalisadorLexico lexer) {
        this.lexer = lexer;
        tokenAtual = lerToken();
        tokenSeguinte = lerToken();
        ultimoTokenConsumido = tokenAtual;
    }

    // ==================== PONTO DE ENTRADA ====================

    public NoAST analisarPrograma() {
        NoAST programa = new NoAST("programa");

        if (ehLexema("package")) {
            analisarDeclaracaoPacote();
        } else {
            relatarErro("'package'", "inicio do programa");
            sincronizarClasse();
        }

        while (ehLexema("import")) {
            analisarDeclaracaoImportacao();
        }

        while (!fim()) {
            int marca = totalAvancos;
            NoAST tipo = analisarDeclaracaoTipo();
            programa.adicionarFilho(tipo);
            garantirProgresso(marca);
        }
        return programa;
    }

    public boolean temErros() {
        return !listaErros.isEmpty();
    }

    public List<ErroSintatico> obterErros() {
        return listaErros;
    }

    public TabelaSimbolos obterTabelaSimbolos() {
        return tabelaSimbolos;
    }

    // ==================== CONSUMO DE TOKENS ====================

    private Token lerToken() {
        Token tokenLido;
        do {
            tokenLido = lexer.analex();
            tokenLido.linha = lexer.getLinhaAtual();
        } while (tokenLido.codigo == AnalisadorLexico.TOKEN_COMENTARIO);
        if (tokenLido.codigo == AnalisadorLexico.TOKEN_ERRO) {
            listaErros.add(new ErroSintatico(tokenLido.linha, "token valido", "'" + tokenLido.lexema + "' (ERRO)", "analise lexica"));
        }
        return tokenLido;
    }

    private void avancar() {
        ultimoTokenConsumido = tokenAtual;
        tokenAtual = tokenSeguinte;
        tokenSeguinte = lerToken();
        totalAvancos++;
    }

    private boolean fim() {
        return tokenAtual.codigo == AnalisadorLexico.TOKEN_FIM_ARQUIVO;
    }

    private boolean ehLexema(String lexema) {
        return lexema.equals(tokenAtual.lexema);
    }

    private boolean ehLexemaSeguinte(String lexema) {
        return lexema.equals(tokenSeguinte.lexema);
    }

    private boolean ehIdentificador() {
        return tokenAtual.codigo == AnalisadorLexico.TOKEN_IDENTIFICADOR
                && !ehPalavraReservada(tokenAtual.lexema);
    }

    private boolean verificarLexema(String lexema) {
        if (ehLexema(lexema)) {
            avancar();
            return true;
        }
        return false;
    }

    private Token consumirLexema(String lexema, String contexto) {
        if (ehLexema(lexema)) {
            Token tokenConsumido = tokenAtual;
            avancar();
            return tokenConsumido;
        }
        relatarSimboloEmFalta("'" + lexema + "'", contexto);
        // Recuperacao local: se o token esperado aparece logo a seguir, salta o intruso.
        if (ehLexemaSeguinte(lexema)) {
            avancar();
            Token tokenConsumido = tokenAtual;
            avancar();
            return tokenConsumido;
        }
        return sintetico(lexema);
    }

    private Token consumirIdentificador(String contexto) {
        if (ehIdentificador()) {
            Token tokenConsumido = tokenAtual;
            avancar();
            return tokenConsumido;
        }
        relatarErro("identificador", contexto);
        Token tokenSintetico = sintetico("<missing>");
        if (!fim() && !ehLexema(";") && !ehLexema(")") && !ehLexema("}") && !ehLexema(",")) {
            avancar();
        }
        return tokenSintetico;
    }

    private Token sintetico(String lexema) {
        return new Token(lexema, AnalisadorLexico.TOKEN_IDENTIFICADOR, tokenAtual.linha);
    }

    // ==================== REGISTO DE ERROS ====================

    /**
     * Regista um erro no token ATUAL (o token errado que foi realmente encontrado).
     * Usado quando o problema e o proprio token presente (palavra inesperada,
     * falta de uma categoria como "identificador" ou "expressao").
     */
    private void relatarErro(String esperado, String contexto) {
        registrarErro(tokenAtual.linha, esperado, descrever(tokenAtual), contexto, "");
    }

    /**
     * Regista a falta de um simbolo. Se for um terminador (";", ")", "]", "}", ":"),
     * a linha do erro passa a ser a do ULTIMO token valido consumido -- isto e, o
     * fim da construcao onde o simbolo devia estar -- e nao a linha onde a analise
     * parou. Assim corrige-se o problema de o erro "saltar" para linhas seguintes.
     */
    private void relatarSimboloEmFalta(String esperado, String contexto) {
        String semAspas = esperado.replace("'", "");
        if (TERMINADORES.contains(semAspas)) {
            registrarErro(ultimoTokenConsumido.linha, esperado, descrever(tokenAtual), contexto, ultimoTokenConsumido.lexema);
        } else {
            registrarErro(tokenAtual.linha, esperado, descrever(tokenAtual), contexto, "");
        }
    }

    private void registrarErro(int linha, String esperado, String recebido, String contexto, String apos) {
        String chave = linha + "|" + esperado + "|" + recebido + "|" + contexto;
        if (!chave.equals(ultimoErro)) {
            listaErros.add(new ErroSintatico(linha, esperado, recebido, contexto, apos));
            ultimoErro = chave;
        }
    }

    private String descrever(Token token) {
        return "'" + token.lexema + "' (" + AnalisadorLexico.getNomeToken(token.codigo) + ")";
    }

    // ==================== SINCRONIZACAO POR CONTEXTO ====================
    //
    // avancarAte() e apenas o mecanismo de baixo nivel (avancar ate um dos tokens
    // de paragem). Cada contexto tem o seu proprio metodo com o seu proprio
    // conjunto de tokens de sincronizacao. O erro ja foi registado ANTES de
    // qualquer chamada a estes metodos.

    private void avancarAte(Set<String> paragem) {
        while (!fim() && !paragem.contains(tokenAtual.lexema)) {
            avancar();
        }
    }

    /** Recupera ate ao inicio da proxima classe (ou fim de ficheiro). */
    private void sincronizarClasse() {
        avancarAte(SINCRONIZACAO_CLASSE);
    }

    /** Recupera ate ao proximo membro da classe ou ao fecho "}" da classe. */
    private void sincronizarMetodo() {
        avancarAte(SINCRONIZACAO_MEMBRO);
    }

    /** Recupera ate ao fim de uma declaracao ";" (consumindo-o) ou ao proximo membro. */
    private void sincronizarDeclaracao() {
        avancarAte(SINCRONIZACAO_DECLARACAO);
        if (ehLexema(";")) {
            avancar();
        }
    }

    /** Recupera ate ao fecho de bloco "}" ou ao inicio do proximo comando. */
    private void sincronizarBloco() {
        avancarAte(SINCRONIZACAO_BLOCO);
    }

    /** Recupera dentro de uma sequencia de comandos, consumindo o ";" se existir. */
    private void sincronizarComando() {
        avancarAte(SINCRONIZACAO_BLOCO);
        if (ehLexema(";")) {
            avancar();
        }
    }

    /** Recupera dentro de uma expressao, parando em delimitadores. */
    private void sincronizarExpressao() {
        avancarAte(SINCRONIZACAO_EXPRESSAO);
    }

    /** Recupera dentro de uma lista de parametros. */
    private void sincronizarParametros() {
        avancarAte(SINCRONIZACAO_PARAMETROS);
    }

    /** Recupera cabecalhos de estruturas de controlo (if/while/for). */
    private void sincronizarEstrutura() {
        avancarAte(SINCRONIZACAO_ESTRUTURA);
    }

    /**
     * Garante que cada iteracao de um ciclo de analise consome pelo menos um
     * token. Sem esta guarda, um token que pertenca ao conjunto de sincronizacao
     * mas nao seja consumido pela regra poderia originar um ciclo infinito.
     */
    private void garantirProgresso(int marcaInicial) {
        if (totalAvancos == marcaInicial && !fim()) {
            avancar();
        }
    }

    private void consumirFimDeclaracao(String contexto) {
        if (verificarLexema(";")) {
            return;
        }
        relatarSimboloEmFalta("';'", contexto);
        sincronizarDeclaracao();
    }

    // ==================== PACOTE E IMPORTACOES ====================

    private void analisarDeclaracaoPacote() {
        consumirLexema("package", "declaracao de pacote");
        analisarNomeQualificado("declaracao de pacote");
        consumirFimDeclaracao("declaracao de pacote");
    }

    private void analisarDeclaracaoImportacao() {
        consumirLexema("import", "declaracao de importacao");
        consumirIdentificador("declaracao de importacao");
        while (verificarLexema(".")) {
            if (verificarLexema("*")) {
                break;
            }
            consumirIdentificador("declaracao de importacao");
        }
        consumirFimDeclaracao("declaracao de importacao");
    }

    private String analisarNomeQualificado(String contexto) {
        StringBuilder nome = new StringBuilder();
        Token primeiro = consumirIdentificador(contexto);
        nome.append(primeiro.lexema);
        while (verificarLexema(".")) {
            Token parte = consumirIdentificador(contexto);
            nome.append(".").append(parte.lexema);
        }
        return nome.toString();
    }

    // ==================== DECLARACAO DE TIPOS/CLASSES ====================

    private NoAST analisarDeclaracaoTipo() {
        List<String> modificadores = analisarModificadores();
        if (ehLexema("class")) {
            return analisarDeclaracaoClasse(modificadores);
        }
        relatarErro("'class'", "declaracao de tipo");
        sincronizarClasse();
        return null;
    }

    private List<String> analisarModificadores() {
        List<String> modificadores = new ArrayList<String>();
        while (ehModificador(tokenAtual.lexema)) {
            modificadores.add(tokenAtual.lexema);
            avancar();
        }
        return modificadores;
    }

    private NoAST analisarDeclaracaoClasse(List<String> modificadores) {
        consumirLexema("class", "declaracao de classe");
        Token nomeClasse = consumirIdentificador("declaracao de classe");

        Simbolo simbolo = new Simbolo(AnalisadorLexico.getNomeToken(nomeClasse.codigo), nomeClasse.lexema, nomeClasse.linha, "classe");
        simbolo.obterModificadores().addAll(modificadores);
        simbolo.definirTipoDado(nomeClasse.lexema);
        declarar(simbolo);

        NoAST noClasse = new NoAST("classe", nomeClasse.lexema, nomeClasse.linha);

        if (verificarLexema("extends")) {
            analisarTipo();
        }

        consumirLexema("{", "corpo da classe");
        tabelaSimbolos.entrarEscopo(nomeClasse.lexema, "classe");
        while (!fim() && !ehLexema("}")) {
            int marca = totalAvancos;
            analisarMembroClasse(noClasse);
            garantirProgresso(marca);
        }
        consumirLexema("}", "corpo da classe");
        tabelaSimbolos.sairEscopo();
        return noClasse;
    }

    private void analisarMembroClasse(NoAST noClasse) {
        List<String> modificadores = analisarModificadores();
        if (!ehInicioTipo(true)) {
            relatarErro("declaracao de atributo ou metodo", "membro da classe");
            sincronizarMetodo();
            return;
        }

        InformacaoTipo tipo = analisarTipoOuVazio();
        Token nome = consumirIdentificador("membro da classe");

        if (verificarLexema("(")) {
            noClasse.adicionarFilho(analisarRestanteMetodo(modificadores, tipo, nome));
        } else {
            analisarRestanteCampo(modificadores, tipo, nome, noClasse);
        }
    }

    private NoAST analisarRestanteMetodo(List<String> modificadores, InformacaoTipo tipo, Token nome) {
        Simbolo metodo = new Simbolo(AnalisadorLexico.getNomeToken(nome.codigo), nome.lexema, nome.linha, "metodo");
        metodo.obterModificadores().addAll(modificadores);
        metodo.definirTipoRetorno(tipo.comoTexto());
        metodo.definirTipoDado(tipo.comoTexto());
        declarar(metodo);

        NoAST noMetodo = new NoAST("metodo", nome.lexema, nome.linha);
        noMetodo.definirTipo(tipo.comoTexto());

        tabelaSimbolos.entrarEscopo(nome.lexema, "metodo");
        analisarListaParametros(metodo, noMetodo);
        analisarCorpoMetodo(noMetodo);
        tabelaSimbolos.sairEscopo();
        return noMetodo;
    }

    private void analisarListaParametros(Simbolo metodo, NoAST noMetodo) {
        if (!ehLexema(")")) {
            analisarParametro(metodo, noMetodo);
            while (verificarLexema(",")) {
                analisarParametro(metodo, noMetodo);
            }
        }
        if (!verificarLexema(")")) {
            relatarSimboloEmFalta("')'", "lista de parametros");
            sincronizarParametros();
            verificarLexema(")");
        }
    }

    private void analisarParametro(Simbolo metodo, NoAST noMetodo) {
        InformacaoTipo tipo = analisarTipo();
        Token nome = consumirIdentificador("parametro");
        metodo.obterParametros().add(tipo.comoTexto() + " " + nome.lexema);

        Simbolo parametro = new Simbolo(AnalisadorLexico.getNomeToken(nome.codigo), nome.lexema, nome.linha, "parametro");
        parametro.definirTipoDado(tipo.comoTexto());
        parametro.definirTipoVariavel("local");
        parametro.definirInicializado(true);
        parametro.definirDimensoes(tipo.obterDimensoes());
        declarar(parametro);

        NoAST noParametro = new NoAST("parametro", nome.lexema, nome.linha);
        noParametro.definirTipo(tipo.comoTexto());
        noMetodo.adicionarFilho(noParametro);
    }

    private void analisarRestanteCampo(List<String> modificadores, InformacaoTipo tipo, Token primeiroNome, NoAST noClasse) {
        noClasse.adicionarFilho(declararVariavel(primeiroNome, tipo, modificadores, "atributo", "atributo"));
        while (verificarLexema(",")) {
            Token nome = consumirIdentificador("declaracao de atributo");
            noClasse.adicionarFilho(declararVariavel(nome, tipo, modificadores, "atributo", "atributo"));
        }
        consumirFimDeclaracao("declaracao de atributo");
    }

    // ==================== CORPO DE METODO / BLOCOS ====================

    private void analisarCorpoMetodo(NoAST noMetodo) {
        consumirLexema("{", "corpo do metodo");
        while (!fim() && !ehLexema("}")) {
            int marca = totalAvancos;
            noMetodo.adicionarFilho(analisarComando());
            garantirProgresso(marca);
        }
        consumirLexema("}", "corpo do metodo");
    }

    private NoAST analisarBloco() {
        NoAST noBloco = new NoComando("bloco", "", tokenAtual.linha);
        consumirLexema("{", "bloco");
        tabelaSimbolos.entrarEscopo("bloco" + (++contadorBlocos), "bloco");
        while (!fim() && !ehLexema("}")) {
            int marca = totalAvancos;
            noBloco.adicionarFilho(analisarComando());
            garantirProgresso(marca);
        }
        consumirLexema("}", "bloco");
        tabelaSimbolos.sairEscopo();
        return noBloco;
    }

    // ==================== COMANDOS ====================

    private NoAST analisarComando() {
        if (ehLexema("{")) {
            return analisarBloco();
        }
        if (ehLexema("if")) {
            return analisarComandoSe();
        }
        if (ehLexema("while")) {
            return analisarComandoEnquanto();
        }
        if (ehLexema("for")) {
            return analisarComandoPara();
        }
        if (ehLexema("return")) {
            return analisarComandoRetorno();
        }
        if (ehLexema("break")) {
            NoAST no = new NoComando("break", "break", tokenAtual.linha);
            consumirLexema("break", "comando break");
            consumirFimDeclaracao("comando break");
            return no;
        }
        if (ehLexema("continue")) {
            NoAST no = new NoComando("continue", "continue", tokenAtual.linha);
            consumirLexema("continue", "comando continue");
            consumirFimDeclaracao("comando continue");
            return no;
        }

        if (ehInicioDeclaracao()) {
            return analisarDeclaracaoVariavelLocal(true);
        }

        NoAST expressao = analisarExpressao();
        NoAST comando = new NoComando("comandoExpressao", "", expressao.obterLinha());
        comando.adicionarFilho(expressao);
        if (!verificarLexema(";")) {
            relatarSimboloEmFalta("';'", "comando de expressao");
            sincronizarComando();
        }
        return comando;
    }

    private NoAST analisarComandoSe() {
        Token inicio = consumirLexema("if", "comando if");
        NoAST no = new NoComando("se", "if", inicio.linha);
        consumirLexema("(", "condicao if");
        no.adicionarFilho(analisarExpressao());
        if (!verificarLexema(")")) {
            relatarSimboloEmFalta("')'", "condicao if");
            sincronizarEstrutura();
            verificarLexema(")");
        }
        no.adicionarFilho(analisarComando());
        if (verificarLexema("else")) {
            no.adicionarFilho(analisarComando());
        }
        return no;
    }

    private NoAST analisarComandoEnquanto() {
        Token inicio = consumirLexema("while", "comando while");
        NoAST no = new NoComando("enquanto", "while", inicio.linha);
        consumirLexema("(", "condicao while");
        no.adicionarFilho(analisarExpressao());
        if (!verificarLexema(")")) {
            relatarSimboloEmFalta("')'", "condicao while");
            sincronizarEstrutura();
            verificarLexema(")");
        }
        no.adicionarFilho(analisarComando());
        return no;
    }

    private NoAST analisarComandoPara() {
        Token inicio = consumirLexema("for", "comando for");
        NoAST no = new NoComando("para", "for", inicio.linha);
        consumirLexema("(", "cabecalho for");

        // Inicializador
        if (ehLexema(";")) {
            no.adicionarFilho(new NoComando("vazio", "", tokenAtual.linha));
        } else if (ehInicioDeclaracao()) {
            no.adicionarFilho(analisarDeclaracaoVariavelLocal(false));
        } else {
            NoAST exprInit = analisarExpressao();
            NoAST comandoInit = new NoComando("comandoExpressao", "", exprInit.obterLinha());
            comandoInit.adicionarFilho(exprInit);
            no.adicionarFilho(comandoInit);
        }
        consumirLexema(";", "inicializador for");

        // Condicao
        if (ehLexema(";")) {
            no.adicionarFilho(new NoComando("vazio", "", tokenAtual.linha));
        } else {
            no.adicionarFilho(analisarExpressao());
        }
        consumirLexema(";", "condicao for");

        // Atualizacao
        if (ehLexema(")")) {
            no.adicionarFilho(new NoComando("vazio", "", tokenAtual.linha));
        } else {
            no.adicionarFilho(analisarExpressao());
        }
        if (!verificarLexema(")")) {
            relatarSimboloEmFalta("')'", "atualizacao for");
            sincronizarEstrutura();
            verificarLexema(")");
        }
        no.adicionarFilho(analisarComando());
        return no;
    }

    private NoAST analisarComandoRetorno() {
        Token inicio = consumirLexema("return", "comando return");
        NoAST no = new NoComando("retorno", "return", inicio.linha);
        if (!ehLexema(";")) {
            no.adicionarFilho(analisarExpressao());
        }
        consumirFimDeclaracao("comando return");
        return no;
    }

    private NoAST analisarDeclaracaoVariavelLocal(boolean consumirPontoEVirgula) {
        InformacaoTipo tipo = analisarTipo();
        NoAST grupo = new NoComando("declaracoes", "", tokenAtual.linha);

        Token nome = consumirIdentificador("declaracao de variavel local");
        grupo.adicionarFilho(declararVariavel(nome, tipo, new ArrayList<String>(), "declaracaoLocal", "local"));

        while (verificarLexema(",")) {
            Token outro = consumirIdentificador("declaracao de variavel local");
            grupo.adicionarFilho(declararVariavel(outro, tipo, new ArrayList<String>(), "declaracaoLocal", "local"));
        }

        if (consumirPontoEVirgula) {
            consumirFimDeclaracao("declaracao de variavel local");
        }
        return grupo;
    }

    /**
     * Declara uma variavel/atributo na tabela de simbolos e devolve o no da AST
     * que a representa. O no guarda o nome (lexema), o tipo declarado (definirTipo)
     * e, se existir, a expressao de inicializacao como filho.
     */
    private NoAST declararVariavel(Token nome, InformacaoTipo tipoBase, List<String> modificadores, String tipoNo, String tipoVariavel) {
        int dimensoesExtras = analisarSufixoArrayDeclarador();
        InformacaoTipo tipo = new InformacaoTipo(tipoBase.obterNome(), tipoBase.obterDimensoes() + dimensoesExtras);

        Simbolo simbolo = new Simbolo(AnalisadorLexico.getNomeToken(nome.codigo), nome.lexema, nome.linha, tipoVariavel.equals("atributo") ? "atributo" : "variavel");
        simbolo.definirTipoDado(tipo.comoTexto());
        simbolo.definirTipoVariavel(tipoVariavel);
        simbolo.definirDimensoes(tipo.obterDimensoes());
        simbolo.obterModificadores().addAll(modificadores);

        NoAST noDeclaracao = new NoComando(tipoNo, nome.lexema, nome.linha);
        noDeclaracao.definirTipo(tipo.comoTexto());

        if (verificarLexema("=")) {
            simbolo.definirInicializado(true);
            noDeclaracao.adicionarFilho(analisarInicializador());
        }

        declarar(simbolo);
        return noDeclaracao;
    }

    private NoAST analisarInicializador() {
        if (ehLexema("{")) {
            NoAST no = new NoExpressao("inicializadorArray", "{...}", tokenAtual.linha);
            consumirLexema("{", "inicializador de array");
            while (!fim() && !ehLexema("}")) {
                if (!ehLexema(",")) {
                    no.adicionarFilho(analisarExpressao());
                }
                verificarLexema(",");
            }
            consumirLexema("}", "inicializador de array");
            return no;
        }
        return analisarExpressao();
    }

    private void declarar(Simbolo simbolo) {
        // O parser apenas TENTA declarar; a deteccao de redeclaracao (erro
        // semantico) e feita pelo AnalisadorSemantico a partir da AST.
        tabelaSimbolos.declarar(simbolo);
    }

    // ==================== TIPOS ====================

    private InformacaoTipo analisarTipoOuVazio() {
        if (verificarLexema("void")) {
            return new InformacaoTipo("void", 0);
        }
        return analisarTipo();
    }

    private InformacaoTipo analisarTipo() {
        String nome;
        if (ehTipoPrimitivo(tokenAtual.lexema) || ehLexema("String")) {
            nome = tokenAtual.lexema;
            avancar();
        } else {
            nome = analisarNomeQualificado("tipo");
        }
        int dimensoes = analisarSufixoArrayTipo();
        return new InformacaoTipo(nome, dimensoes);
    }

    private int analisarSufixoArrayTipo() {
        int dimensoes = 0;
        while (verificarLexema("[")) {
            consumirLexema("]", "tipo array");
            dimensoes++;
        }
        return dimensoes;
    }

    private int analisarSufixoArrayDeclarador() {
        int dimensoes = 0;
        while (verificarLexema("[")) {
            consumirLexema("]", "declarador de array");
            dimensoes++;
        }
        return dimensoes;
    }

    private boolean ehInicioTipo(boolean permitirVazio) {
        return (permitirVazio && ehLexema("void")) || ehTipoPrimitivo(tokenAtual.lexema)
                || ehLexema("String") || ehIdentificador();
    }

    private boolean ehInicioDeclaracao() {
        if (ehTipoPrimitivo(tokenAtual.lexema) || ehLexema("String")) {
            return true;
        }
        return ehIdentificador() && tokenSeguinte.codigo == AnalisadorLexico.TOKEN_IDENTIFICADOR;
    }

    // ==================== EXPRESSOES ====================
    // Cada nivel de precedencia constroi o seu no da AST. As operacoes binarias
    // sao associativas a esquerda; a atribuicao e o operador ternario sao
    // associativos a direita.

    private NoAST analisarExpressao() {
        return analisarExpressaoAtribuicao();
    }

    private NoAST analisarExpressaoAtribuicao() {
        NoAST esquerda = analisarExpressaoCondicional();
        if (ehOperadorAtribuicao(tokenAtual.lexema)) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            NoAST direita = analisarExpressaoAtribuicao();
            NoAST no = new NoExpressao("atribuicao", operador, linha);
            no.adicionarFilho(esquerda);
            no.adicionarFilho(direita);
            return no;
        }
        return esquerda;
    }

    private NoAST analisarExpressaoCondicional() {
        NoAST condicao = analisarExpressaoOrLogico();
        if (ehLexema("?")) {
            int linha = tokenAtual.linha;
            avancar();
            NoAST entao = analisarExpressao();
            consumirLexema(":", "expressao condicional");
            NoAST senao = analisarExpressaoCondicional();
            NoAST no = new NoExpressao("ternario", "?:", linha);
            no.adicionarFilho(condicao);
            no.adicionarFilho(entao);
            no.adicionarFilho(senao);
            return no;
        }
        return condicao;
    }

    private NoAST analisarExpressaoOrLogico() {
        NoAST no = analisarExpressaoELogico();
        while (ehLexema("||")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoELogico());
        }
        return no;
    }

    private NoAST analisarExpressaoELogico() {
        NoAST no = analisarExpressaoOrBit();
        while (ehLexema("&&")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoOrBit());
        }
        return no;
    }

    private NoAST analisarExpressaoOrBit() {
        NoAST no = analisarExpressaoXorBit();
        while (ehLexema("|")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoXorBit());
        }
        return no;
    }

    private NoAST analisarExpressaoXorBit() {
        NoAST no = analisarExpressaoAndBit();
        while (ehLexema("^")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoAndBit());
        }
        return no;
    }

    private NoAST analisarExpressaoAndBit() {
        NoAST no = analisarExpressaoIgualdade();
        while (ehLexema("&")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoIgualdade());
        }
        return no;
    }

    private NoAST analisarExpressaoIgualdade() {
        NoAST no = analisarExpressaoRelacional();
        while (ehLexema("==") || ehLexema("!=")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoRelacional());
        }
        return no;
    }

    private NoAST analisarExpressaoRelacional() {
        NoAST no = analisarExpressaoAditiva();
        while (ehLexema("<") || ehLexema(">") || ehLexema("<=") || ehLexema(">=")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoAditiva());
        }
        return no;
    }

    private NoAST analisarExpressaoAditiva() {
        NoAST no = analisarExpressaoMultiplicativa();
        while (ehLexema("+") || ehLexema("-")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoMultiplicativa());
        }
        return no;
    }

    private NoAST analisarExpressaoMultiplicativa() {
        NoAST no = analisarExpressaoUnaria();
        while (ehLexema("*") || ehLexema("/") || ehLexema("%")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            no = binario(operador, linha, no, analisarExpressaoUnaria());
        }
        return no;
    }

    /** Constroi um no de operacao binaria com os dois operandos ja analisados. */
    private NoAST binario(String operador, int linha, NoAST esquerda, NoAST direita) {
        NoAST no = new NoExpressao("binario", operador, linha);
        no.adicionarFilho(esquerda);
        no.adicionarFilho(direita);
        return no;
    }

    private NoAST analisarExpressaoUnaria() {
        if (ehLexema("+") || ehLexema("-") || ehLexema("!") || ehLexema("++") || ehLexema("--")) {
            String operador = tokenAtual.lexema;
            int linha = tokenAtual.linha;
            avancar();
            NoAST operando = analisarExpressaoUnaria();
            NoAST no = new NoExpressao("unario", operador, linha);
            no.adicionarFilho(operando);
            return no;
        }
        return analisarExpressaoPosfixa();
    }

    private NoAST analisarExpressaoPosfixa() {
        NoAST no = analisarExpressaoPrimaria();
        while (true) {
            if (ehLexema("[")) {
                int linha = tokenAtual.linha;
                avancar();
                NoAST indice = analisarExpressao();
                if (!verificarLexema("]")) {
                    relatarSimboloEmFalta("']'", "acesso a array");
                    sincronizarExpressao();
                    verificarLexema("]");
                }
                NoAST acesso = new NoExpressao("acessoArray", "[]", linha);
                acesso.adicionarFilho(no);
                acesso.adicionarFilho(indice);
                no = acesso;
            } else if (ehLexema("(")) {
                int linha = tokenAtual.linha;
                avancar();
                List<NoAST> argumentos = analisarListaArgumentos();
                if (!verificarLexema(")")) {
                    relatarSimboloEmFalta("')'", "chamada de metodo");
                    sincronizarExpressao();
                    verificarLexema(")");
                }
                NoAST chamada = new NoExpressao("chamada", nomeChamavel(no), linha);
                chamada.adicionarFilho(no);
                for (NoAST argumento : argumentos) {
                    chamada.adicionarFilho(argumento);
                }
                no = chamada;
            } else if (ehLexema(".")) {
                avancar();
                Token campo = consumirIdentificador("acesso a atributo");
                NoAST acesso = new NoExpressao("acessoMembro", campo.lexema, campo.linha);
                acesso.adicionarFilho(no);
                no = acesso;
            } else if (ehLexema("++") || ehLexema("--")) {
                NoAST pos = new NoExpressao("posIncremento", tokenAtual.lexema, tokenAtual.linha);
                pos.adicionarFilho(no);
                avancar();
                no = pos;
            } else {
                return no;
            }
        }
    }

    private String nomeChamavel(NoAST no) {
        if (no != null && ("identificador".equals(no.obterNome()) || "acessoMembro".equals(no.obterNome()))) {
            return no.obterLexema();
        }
        return "";
    }

    private List<NoAST> analisarListaArgumentos() {
        List<NoAST> argumentos = new ArrayList<NoAST>();
        if (ehLexema(")")) {
            return argumentos;
        }
        argumentos.add(analisarExpressao());
        while (verificarLexema(",")) {
            argumentos.add(analisarExpressao());
        }
        return argumentos;
    }

    private NoAST analisarExpressaoPrimaria() {
        if (ehLiteral()) {
            return criarLiteral();
        }

        if (ehIdentificador()) {
            NoAST no = new NoExpressao("identificador", tokenAtual.lexema, tokenAtual.linha);
            avancar();
            return no;
        }

        if (ehLexema("this") || ehLexema("super")) {
            NoAST no = new NoExpressao(tokenAtual.lexema, tokenAtual.lexema, tokenAtual.linha);
            avancar();
            return no;
        }

        if (ehLexema("(")) {
            avancar();
            NoAST interna = analisarExpressao();
            if (!verificarLexema(")")) {
                relatarSimboloEmFalta("')'", "expressao parentizada");
                sincronizarExpressao();
                verificarLexema(")");
            }
            return interna;
        }

        if (ehLexema("new")) {
            avancar();
            return analisarExpressaoCriacao();
        }

        relatarErro("expressao", "expressao primaria");
        NoAST erro = new NoExpressao("erro", tokenAtual.lexema, tokenAtual.linha);
        sincronizarExpressao();
        if (!fim() && !SINCRONIZACAO_EXPRESSAO.contains(tokenAtual.lexema)) {
            avancar();
        }
        return erro;
    }

    private NoAST criarLiteral() {
        Token token = tokenAtual;
        String tipoNo;
        switch (token.codigo) {
            case AnalisadorLexico.TOKEN_NUMERO_INTEIRO: tipoNo = "literalInt"; break;
            case AnalisadorLexico.TOKEN_NUMERO_REAL: tipoNo = "literalReal"; break;
            case AnalisadorLexico.TOKEN_STRING: tipoNo = "literalString"; break;
            case AnalisadorLexico.TOKEN_CHAR_LITERAL: tipoNo = "literalChar"; break;
            default:
                if ("true".equals(token.lexema) || "false".equals(token.lexema)) {
                    tipoNo = "literalBool";
                } else {
                    tipoNo = "literalNull";
                }
        }
        NoAST no = new NoExpressao(tipoNo, token.lexema, token.linha);
        avancar();
        return no;
    }

    private NoAST analisarExpressaoCriacao() {
        Token tipo = analisarNomeTipoCriacao();
        if (ehLexema("(")) {
            int linha = tokenAtual.linha;
            avancar();
            List<NoAST> argumentos = analisarListaArgumentos();
            if (!verificarLexema(")")) {
                relatarSimboloEmFalta("')'", "criacao de objeto");
                sincronizarExpressao();
                verificarLexema(")");
            }
            NoAST no = new NoExpressao("novoObjeto", tipo.lexema, linha);
            for (NoAST argumento : argumentos) {
                no.adicionarFilho(argumento);
            }
            return no;
        }
        return analisarRestanteCriacaoArray(tipo);
    }

    private Token analisarNomeTipoCriacao() {
        if (ehTipoPrimitivo(tokenAtual.lexema) || ehLexema("String")) {
            Token tipo = tokenAtual;
            avancar();
            return tipo;
        }
        Token primeiro = consumirIdentificador("expressao new");
        while (verificarLexema(".")) {
            consumirIdentificador("expressao new");
        }
        return primeiro;
    }

    private NoAST analisarRestanteCriacaoArray(Token tipo) {
        NoAST no = new NoExpressao("novoArray", tipo.lexema, tipo.linha);
        if (!verificarLexema("[")) {
            relatarErro("'[' ou '('", "expressao new");
            return no;
        }

        no.adicionarFilho(analisarExpressao());
        if (!verificarLexema("]")) {
            relatarSimboloEmFalta("']'", "criacao de array");
            sincronizarExpressao();
            verificarLexema("]");
        }

        while (verificarLexema("[")) {
            if (!ehLexema("]")) {
                no.adicionarFilho(analisarExpressao());
            }
            if (!verificarLexema("]")) {
                relatarSimboloEmFalta("']'", "criacao de array");
                sincronizarExpressao();
                verificarLexema("]");
            }
        }
        return no;
    }

    // ==================== CLASSIFICADORES ====================

    private boolean ehLiteral() {
        return tokenAtual.codigo == AnalisadorLexico.TOKEN_NUMERO_INTEIRO
                || tokenAtual.codigo == AnalisadorLexico.TOKEN_NUMERO_REAL
                || tokenAtual.codigo == AnalisadorLexico.TOKEN_STRING
                || tokenAtual.codigo == AnalisadorLexico.TOKEN_CHAR_LITERAL
                || ehLexema("true") || ehLexema("false") || ehLexema("null");
    }

    private boolean ehTipoPrimitivo(String lexema) {
        return "int".equals(lexema) || "double".equals(lexema) || "boolean".equals(lexema)
                || "char".equals(lexema) || "float".equals(lexema) || "long".equals(lexema);
    }

    private boolean ehModificador(String lexema) {
        return "public".equals(lexema) || "private".equals(lexema) || "protected".equals(lexema)
                || "static".equals(lexema) || "final".equals(lexema);
    }

    private boolean ehOperadorAtribuicao(String lexema) {
        return "=".equals(lexema) || "+=".equals(lexema) || "-=".equals(lexema) || "*=".equals(lexema)
                || "/=".equals(lexema) || "%=".equals(lexema) || "&=".equals(lexema) || "|=".equals(lexema)
                || "^=".equals(lexema);
    }

    private boolean ehPalavraReservada(String lexema) {
        return PALAVRAS_RESERVADAS.contains(lexema);
    }
}
