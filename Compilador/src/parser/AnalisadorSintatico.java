package parser;

import ast.NoAST;
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

public class AnalisadorSintatico {
    private static final Set<String> PALAVRAS_RESERVADAS = new HashSet<String>(Arrays.asList(
            "package", "import", "class", "extends", "public", "private", "protected", "static", "final",
            "void", "if", "else", "while", "for", "return", "break", "continue", "new", "this", "super",
            "true", "false", "null", "int", "double", "boolean", "char", "float", "long", "String"));
    private static final Set<String> SINCRONIZACAO_DECLARACAO = new HashSet<String>(Arrays.asList(
            ";", "}", "class", "public", "private", "protected", "static", "final", "void",
            "int", "double", "boolean", "char", "float", "long", "String"));
    private static final Set<String> SINCRONIZACAO_BLOCO = new HashSet<String>(Arrays.asList(
            "}", "{", "if", "while", "for", "return", "break", "continue", "int", "double",
            "boolean", "char", "float", "long", "String"));
    private static final Set<String> SINCRONIZACAO_EXPRESSAO = new HashSet<String>(Arrays.asList(
            ";", ")", "]", ",", "}", ":", "{"));
    private static final Set<String> SINCRONIZACAO_ESTRUTURA = new HashSet<String>(Arrays.asList(
            ")", "{", "}", ";", "else"));

    private final AnalisadorLexico lexer;
    private final TabelaSimbolos tabelaSimbolos = new TabelaSimbolos();
    private final List<ErroSintatico> listaErros = new ArrayList<ErroSintatico>();
    private Token tokenAtual;
    private Token tokenSeguinte;
    private int contadorBlocos = 0;
    private String ultimoErro = "";

    public AnalisadorSintatico(AnalisadorLexico lexer) {
        this.lexer = lexer;
        tokenAtual = lerToken();
        tokenSeguinte = lerToken();
    }

    public NoAST analisarPrograma() {
        NoAST programa = new NoAST("programa");
        if (ehLexema("package")) {
            analisarDeclaracaoPacote();
        } else {
            relatarErro("'package'", "inicio do programa");
            sincronizar(new HashSet<String>(Arrays.asList("import", "class", "public", "private", "protected")));
        }

        while (ehLexema("import")) {
            analisarDeclaracaoImportacao();
        }

        while (!fim()) {
            analisarDeclaracaoTipo();
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
        tokenAtual = tokenSeguinte;
        tokenSeguinte = lerToken();
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
        relatarErro("'" + lexema + "'", contexto);
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

    private void relatarErro(String esperado, String contexto) {
        String chave = tokenAtual.linha + "|" + esperado + "|" + tokenAtual.lexema + "|" + contexto;
        if (!chave.equals(ultimoErro)) {
            listaErros.add(new ErroSintatico(tokenAtual.linha, esperado, descrever(tokenAtual), contexto));
            ultimoErro = chave;
        }
    }

    private String descrever(Token token) {
        return "'" + token.lexema + "' (" + AnalisadorLexico.getNomeToken(token.codigo) + ")";
    }

    private void sincronizar(Set<String> sincronizacao) {
        while (!fim() && !sincronizacao.contains(tokenAtual.lexema)) {
            avancar();
        }
    }

    private void sincronizarComando() {
        sincronizar(SINCRONIZACAO_BLOCO);
        if (ehLexema(";")) {
            avancar();
        }
    }

    private void sincronizarDeclaracao() {
        sincronizar(SINCRONIZACAO_DECLARACAO);
        if (ehLexema(";")) {
            avancar();
        }
    }

    private void sincronizarExpressao() {
        sincronizar(SINCRONIZACAO_EXPRESSAO);
    }

    private void sincronizarEstrutura() {
        sincronizar(SINCRONIZACAO_ESTRUTURA);
    }

    private void consumirFimDeclaracao(String contexto) {
        if (verificarLexema(";")) {
            return;
        }
        relatarErro("';'", contexto);
        sincronizarDeclaracao();
    }

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

    private void analisarDeclaracaoTipo() {
        List<String> modificadores = analisarModificadores();
        if (ehLexema("class")) {
            analisarDeclaracaoClasse(modificadores);
            return;
        }
        relatarErro("'class'", "declaracao de tipo");
        sincronizarDeclaracao();
    }

    private List<String> analisarModificadores() {
        List<String> modificadores = new ArrayList<String>();
        while (ehModificador(tokenAtual.lexema)) {
            modificadores.add(tokenAtual.lexema);
            avancar();
        }
        return modificadores;
    }

    private void analisarDeclaracaoClasse(List<String> modificadores) {
        consumirLexema("class", "declaracao de classe");
        Token nomeClasse = consumirIdentificador("declaracao de classe");
        Simbolo simbolo = new Simbolo(AnalisadorLexico.getNomeToken(nomeClasse.codigo), nomeClasse.lexema, nomeClasse.linha, "classe");
        simbolo.obterModificadores().addAll(modificadores);
        simbolo.definirTipoDado(nomeClasse.lexema);
        simbolo.definirTamanho(tabelaSimbolos.tamanhoSimbolo(nomeClasse.lexema));
        declarar(simbolo, "declaracao de classe");

        if (verificarLexema("extends")) {
            analisarTipo();
        }

        consumirLexema("{", "corpo da classe");
        tabelaSimbolos.entrarEscopo(nomeClasse.lexema, "classe");
        while (!fim() && !ehLexema("}")) {
            analisarMembroClasse();
        }
        consumirLexema("}", "corpo da classe");
        tabelaSimbolos.sairEscopo();
    }

    private void analisarMembroClasse() {
        List<String> modificadores = analisarModificadores();
        if (!ehInicioTipo(true)) {
            relatarErro("declaracao de atributo ou metodo", "membro da classe");
            sincronizarDeclaracao();
            return;
        }

        InformacaoTipo tipo = analisarTipoOuVazio();
        Token nome = consumirIdentificador("membro da classe");

        if (verificarLexema("(")) {
            analisarRestanteMetodo(modificadores, tipo, nome);
        } else {
            analisarRestanteCampo(modificadores, tipo, nome);
        }
    }

    private void analisarRestanteMetodo(List<String> modificadores, InformacaoTipo tipo, Token nome) {
        Simbolo metodo = new Simbolo(AnalisadorLexico.getNomeToken(nome.codigo), nome.lexema, nome.linha, "metodo");
        metodo.obterModificadores().addAll(modificadores);
        metodo.definirTipoRetorno(tipo.comoTexto());
        metodo.definirTipoDado(tipo.comoTexto());
        metodo.definirTamanho(0);
        declarar(metodo, "declaracao de metodo");

        tabelaSimbolos.entrarEscopo(nome.lexema, "metodo");
        analisarListaParametros(metodo);
        analisarCorpoMetodo();
        tabelaSimbolos.sairEscopo();
    }

    private void analisarListaParametros(Simbolo metodo) {
        if (!ehLexema(")")) {
            analisarParametro(metodo);
            while (verificarLexema(",")) {
                analisarParametro(metodo);
            }
        }
        consumirLexema(")", "lista de parametros");
    }

    private void analisarParametro(Simbolo metodo) {
        InformacaoTipo tipo = analisarTipo();
        Token nome = consumirIdentificador("parametro");
        metodo.obterParametros().add(tipo.comoTexto() + " " + nome.lexema);

        Simbolo parametro = new Simbolo(AnalisadorLexico.getNomeToken(nome.codigo), nome.lexema, nome.linha, "parametro");
        parametro.definirTipoDado(tipo.comoTexto());
        parametro.definirTipoVariavel("local");
        parametro.definirInicializado(true);
        parametro.definirDimensoes(tipo.obterDimensoes());
        parametro.definirTamanho(tabelaSimbolos.tamanhoSimbolo(tipo.comoTexto()));
        declarar(parametro, "parametro");
    }

    private void analisarRestanteCampo(List<String> modificadores, InformacaoTipo tipo, Token primeiroNome) {
        declararVariavel(primeiroNome, tipo, modificadores, "variavel", "atributo");
        while (verificarLexema(",")) {
            Token nome = consumirIdentificador("declaracao de atributo");
            declararVariavel(nome, tipo, modificadores, "variavel", "atributo");
        }
        consumirFimDeclaracao("declaracao de atributo");
    }

    private void analisarCorpoMetodo() {
        consumirLexema("{", "corpo do metodo");
        while (!fim() && !ehLexema("}")) {
            analisarComando();
        }
        consumirLexema("}", "corpo do metodo");
    }

    private void analisarBloco() {
        consumirLexema("{", "bloco");
        tabelaSimbolos.entrarEscopo("bloco" + (++contadorBlocos), "bloco");
        while (!fim() && !ehLexema("}")) {
            analisarComando();
        }
        consumirLexema("}", "bloco");
        tabelaSimbolos.sairEscopo();
    }

    private void analisarComando() {
        if (ehLexema("{")) {
            analisarBloco();
            return;
        }
        if (ehLexema("if")) {
            analisarComandoSe();
            return;
        }
        if (ehLexema("while")) {
            analisarComandoEnquanto();
            return;
        }
        if (ehLexema("for")) {
            analisarComandoPara();
            return;
        }
        if (ehLexema("return")) {
            analisarComandoRetorno();
            return;
        }
        if (ehLexema("break")) {
            consumirLexema("break", "comando break");
            consumirFimDeclaracao("comando break");
            return;
        }
        if (ehLexema("continue")) {
            consumirLexema("continue", "comando continue");
            consumirFimDeclaracao("comando continue");
            return;
        }

        if (ehInicioDeclaracao()) {
            analisarDeclaracaoVariavelLocal(true);
        } else {
            analisarExpressao();
            if (!verificarLexema(";")) {
                relatarErro("';'", "comando de expressao");
                sincronizarComando();
            }
        }
    }

    private void analisarComandoSe() {
        consumirLexema("if", "comando if");
        consumirLexema("(", "condicao if");
        analisarExpressao();
        if (!verificarLexema(")")) {
            relatarErro("')'", "condicao if");
            sincronizarEstrutura();
            verificarLexema(")");
        }
        analisarComando();
        if (verificarLexema("else")) {
            analisarComando();
        }
    }

    private void analisarComandoEnquanto() {
        consumirLexema("while", "comando while");
        consumirLexema("(", "condicao while");
        analisarExpressao();
        if (!verificarLexema(")")) {
            relatarErro("')'", "condicao while");
            sincronizarEstrutura();
            verificarLexema(")");
        }
        analisarComando();
    }

    private void analisarComandoPara() {
        consumirLexema("for", "comando for");
        consumirLexema("(", "cabecalho for");

        if (!ehLexema(";")) {
            if (ehInicioDeclaracao()) {
                analisarDeclaracaoVariavelLocal(false);
            } else {
                analisarExpressao();
            }
        }
        consumirLexema(";", "inicializador for");

        if (!ehLexema(";")) {
            analisarExpressao();
        }
        consumirLexema(";", "condicao for");

        if (!ehLexema(")")) {
            analisarExpressao();
        }
        consumirLexema(")", "atualizacao for");
        analisarComando();
    }

    private void analisarComandoRetorno() {
        consumirLexema("return", "comando return");
        if (!ehLexema(";")) {
            analisarExpressao();
        }
        consumirFimDeclaracao("comando return");
    }

    private void analisarDeclaracaoVariavelLocal(boolean consumirPontoEVirgula) {
        InformacaoTipo tipo = analisarTipo();
        Token nome = consumirIdentificador("declaracao de variavel local");
        declararVariavel(nome, tipo, new ArrayList<String>(), "variavel", "local");

        while (verificarLexema(",")) {
            Token outro = consumirIdentificador("declaracao de variavel local");
            declararVariavel(outro, tipo, new ArrayList<String>(), "variavel", "local");
        }

        if (consumirPontoEVirgula) {
            consumirFimDeclaracao("declaracao de variavel local");
        }
    }

    private void declararVariavel(Token nome, InformacaoTipo tipoBase, List<String> modificadores, String categoria, String tipoVariavel) {
        int dimensoesExtras = analisarSufixoArrayDeclarador();
        InformacaoTipo tipo = new InformacaoTipo(tipoBase.obterNome(), tipoBase.obterDimensoes() + dimensoesExtras);

        Simbolo simbolo = new Simbolo(AnalisadorLexico.getNomeToken(nome.codigo), nome.lexema, nome.linha, categoria);
        simbolo.definirTipoDado(tipo.comoTexto());
        simbolo.definirTipoVariavel(tipoVariavel);
        simbolo.definirDimensoes(tipo.obterDimensoes());
        simbolo.obterModificadores().addAll(modificadores);
        simbolo.definirTamanho(tabelaSimbolos.tamanhoSimbolo(tipo.comoTexto()));

        if (verificarLexema("=")) {
            simbolo.definirInicializado(true);
            simbolo.definirValor(analisarInicializador());
        }

        declarar(simbolo, categoria);
    }

    private String analisarInicializador() {
        if (verificarLexema("{")) {
            while (!fim() && !ehLexema("}")) {
                if (!ehLexema(",")) {
                    analisarExpressao();
                }
                verificarLexema(",");
            }
            consumirLexema("}", "inicializador de array");
            return "{...}";
        }

        String inicio = tokenAtual.lexema;
        analisarExpressao();
        return inicio;
    }

    private void declarar(Simbolo simbolo, String contexto) {
        if (!tabelaSimbolos.declarar(simbolo)) {
            listaErros.add(new ErroSintatico(simbolo.obterLinha(), "declaracao unica", "'" + simbolo.obterLexema() + "'", contexto + " duplicada"));
        }
    }

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

    private void analisarExpressao() {
        analisarExpressaoAtribuicao();
    }

    private void analisarExpressaoAtribuicao() {
        analisarExpressaoCondicional();
        while (ehOperadorAtribuicao(tokenAtual.lexema)) {
            avancar();
            analisarExpressaoCondicional();
        }
    }

    private void analisarExpressaoCondicional() {
        analisarExpressaoOrLogico();
        if (verificarLexema("?")) {
            analisarExpressao();
            consumirLexema(":", "expressao condicional");
            analisarExpressaoCondicional();
        }
    }

    private void analisarExpressaoOrLogico() {
        analisarExpressaoELogico();
        while (verificarLexema("||")) {
            analisarExpressaoELogico();
        }
    }

    private void analisarExpressaoELogico() {
        analisarExpressaoOrBit();
        while (verificarLexema("&&")) {
            analisarExpressaoOrBit();
        }
    }

    private void analisarExpressaoOrBit() {
        analisarExpressaoXorBit();
        while (verificarLexema("|")) {
            analisarExpressaoXorBit();
        }
    }

    private void analisarExpressaoXorBit() {
        analisarExpressaoAndBit();
        while (verificarLexema("^")) {
            analisarExpressaoAndBit();
        }
    }

    private void analisarExpressaoAndBit() {
        analisarExpressaoIgualdade();
        while (verificarLexema("&")) {
            analisarExpressaoIgualdade();
        }
    }

    private void analisarExpressaoIgualdade() {
        analisarExpressaoRelacional();
        while (ehLexema("==") || ehLexema("!=")) {
            avancar();
            analisarExpressaoRelacional();
        }
    }

    private void analisarExpressaoRelacional() {
        analisarExpressaoAditiva();
        while (ehLexema("<") || ehLexema(">") || ehLexema("<=") || ehLexema(">=")) {
            avancar();
            analisarExpressaoAditiva();
        }
    }

    private void analisarExpressaoAditiva() {
        analisarExpressaoMultiplicativa();
        while (ehLexema("+") || ehLexema("-")) {
            avancar();
            analisarExpressaoMultiplicativa();
        }
    }

    private void analisarExpressaoMultiplicativa() {
        analisarExpressaoUnaria();
        while (ehLexema("*") || ehLexema("/") || ehLexema("%")) {
            avancar();
            analisarExpressaoUnaria();
        }
    }

    private void analisarExpressaoUnaria() {
        while (ehLexema("+") || ehLexema("-") || ehLexema("!") || ehLexema("++") || ehLexema("--")) {
            avancar();
        }
        analisarExpressaoPosfixa();
    }

    private void analisarExpressaoPosfixa() {
        analisarExpressaoPrimaria();
        while (true) {
            if (verificarLexema("[")) {
                analisarExpressao();
                consumirLexema("]", "acesso a array");
            } else if (verificarLexema("(")) {
                analisarListaArgumentos();
                consumirLexema(")", "chamada de metodo");
            } else if (verificarLexema(".")) {
                consumirIdentificador("acesso a atributo");
            } else if (ehLexema("++") || ehLexema("--")) {
                avancar();
            } else {
                return;
            }
        }
    }

    private void analisarListaArgumentos() {
        if (ehLexema(")")) {
            return;
        }
        analisarExpressao();
        while (verificarLexema(",")) {
            analisarExpressao();
        }
    }

    private void analisarExpressaoPrimaria() {
        if (ehLiteral()) {
            avancar();
            return;
        }

        if (ehIdentificador()) {
            Token identificador = tokenAtual;
            Simbolo simbolo = tabelaSimbolos.resolver(identificador.lexema);
            if (simbolo == null && !Character.isUpperCase(identificador.lexema.charAt(0))) {
                listaErros.add(new ErroSintatico(identificador.linha, "identificador declarado", "'" + identificador.lexema + "'", "uso de identificador"));
            }
            avancar();
            return;
        }

        if (verificarLexema("this") || verificarLexema("super")) {
            return;
        }

        if (verificarLexema("(")) {
            analisarExpressao();
            if (!verificarLexema(")")) {
                relatarErro("')'", "expressao parentizada");
                sincronizarExpressao();
                verificarLexema(")");
            }
            return;
        }

        if (verificarLexema("new")) {
            analisarExpressaoCriacao();
            return;
        }

        relatarErro("expressao", "expressao primaria");
        sincronizarExpressao();
        if (!fim() && !SINCRONIZACAO_EXPRESSAO.contains(tokenAtual.lexema)) {
            avancar();
        }
    }

    private void analisarExpressaoCriacao() {
        analisarNomeTipoCriacao();
        if (verificarLexema("(")) {
            analisarListaArgumentos();
            consumirLexema(")", "criacao de objeto");
        } else {
            analisarRestanteCriacaoArray();
        }
    }

    private void analisarNomeTipoCriacao() {
        if (ehTipoPrimitivo(tokenAtual.lexema) || ehLexema("String")) {
            avancar();
        } else {
            analisarNomeQualificado("expressao new");
        }
    }

    private void analisarRestanteCriacaoArray() {
        if (!verificarLexema("[")) {
            relatarErro("'[' ou '('", "expressao new");
            return;
        }

        analisarExpressao();
        if (!verificarLexema("]")) {
            relatarErro("']'", "criacao de array");
            sincronizarExpressao();
            verificarLexema("]");
        }

        while (verificarLexema("[")) {
            if (!ehLexema("]")) {
                analisarExpressao();
            }
            if (!verificarLexema("]")) {
                relatarErro("']'", "criacao de array");
                sincronizarExpressao();
                verificarLexema("]");
            }
        }
    }

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
