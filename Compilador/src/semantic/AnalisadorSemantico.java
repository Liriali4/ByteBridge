package semantic;

import ast.NoAST;
import errors.ErroSemantico;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import symbols.Escopo;
import symbols.Simbolo;
import symbols.TabelaSimbolos;

/**
 * Analisador semantico (Fase 3).
 *
 * E uma camada totalmente separada do parser. Recebe:
 *   - a AST produzida pelo AnalisadorSintatico;
 *   - a TabelaSimbolos construida durante a analise sintatica.
 *
 * Nao volta a ler tokens nem reconstroi a estrutura do programa: limita-se a
 * percorrer a arvore e, apoiando-se na tabela de simbolos, verificar as regras
 * semanticas exigidas pelo enunciado:
 *   1. uso de variaveis nao declaradas;
 *   2. variaveis declaradas duas vezes no mesmo escopo;
 *   3. incompatibilidade de tipos;
 *   4. atribuicoes incompativeis;
 *   5. compatibilidade dos argumentos das chamadas de metodos (numero, tipo, ordem);
 *   6. condicoes de estruturas de controlo (if/while/for) que devem ser boolean.
 *
 * A analise NAO para no primeiro erro: continua sempre que possivel e no fim
 * devolve o relatorio completo. Tipos "desconhecidos" (resultado de erros
 * anteriores) sao propagados sem gerar erros em cascata.
 */
public class AnalisadorSemantico {

    private static final String DESCONHECIDO = "desconhecido";

    // Ordem de alargamento numerico permitido (char -> int -> long -> float -> double).
    private static final List<String> NUMERICOS = Arrays.asList("char", "int", "long", "float", "double");

    private final NoAST raiz;
    private final TabelaSimbolos tabela;
    private final List<ErroSemantico> erros = new ArrayList<ErroSemantico>();

    // Pilha de escopos usada para resolver nomes e detectar redeclaracoes.
    // Cada escopo e um mapa nome -> tipo declarado.
    private final Deque<Map<String, String>> escopos = new ArrayDeque<Map<String, String>>();

    private final Set<String> nomesDeClasses = new HashSet<String>();
    private String tipoRetornoAtual = "void";

    public AnalisadorSemantico(NoAST raiz, TabelaSimbolos tabela) {
        this.raiz = raiz;
        this.tabela = tabela;
    }

    public List<ErroSemantico> analisar() {
        recolherNomesDeClasses();
        entrarEscopo(); // escopo global
        if (raiz != null) {
            for (NoAST filho : raiz.obterFilhos()) {
                if ("classe".equals(filho.obterNome())) {
                    analisarClasse(filho);
                }
            }
        }
        sairEscopo();
        return erros;
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    public List<ErroSemantico> obterErros() {
        return erros;
    }

    private void recolherNomesDeClasses() {
        for (Escopo escopo : tabela.obterEscopos()) {
            for (Simbolo simbolo : escopo.obterSimbolos().values()) {
                if ("classe".equals(simbolo.obterCategoria())) {
                    nomesDeClasses.add(simbolo.obterLexema());
                }
            }
        }
    }

    // ==================== GESTAO DE ESCOPOS ====================

    private void entrarEscopo() {
        escopos.push(new LinkedHashMap<String, String>());
    }

    private void sairEscopo() {
        if (!escopos.isEmpty()) {
            escopos.pop();
        }
    }

    /**
     * Declara um nome no escopo actual. Se ja existir NESTE escopo, gera o erro
     * de dupla declaracao (regra 2). Caso contrario, regista o seu tipo.
     */
    private void declararNoEscopo(String nome, String tipo, int linha, String contexto) {
        Map<String, String> atual = escopos.peek();
        if (atual.containsKey(nome)) {
            registar(linha, nome, "variavel declarada duas vezes",
                    "o identificador '" + nome + "' ja foi declarado neste escopo", contexto);
        } else {
            atual.put(nome, tipo);
        }
    }

    /** Procura o tipo de um nome, do escopo actual para os exteriores. */
    private String resolverTipo(String nome) {
        for (Map<String, String> escopo : escopos) {
            if (escopo.containsKey(nome)) {
                return escopo.get(nome);
            }
        }
        return null;
    }

    private boolean declaradoNoEscopoAtual(String nome) {
        return escopos.peek().containsKey(nome);
    }

    // ==================== CLASSES / METODOS ====================

    private void analisarClasse(NoAST noClasse) {
        entrarEscopo(); // escopo da classe

        // 1.o passo: registar todos os atributos (campos), para que qualquer
        // metodo os possa referenciar independentemente da ordem de escrita.
        for (NoAST membro : noClasse.obterFilhos()) {
            if ("atributo".equals(membro.obterNome())) {
                declararNoEscopo(membro.obterLexema(), membro.obterTipo(), membro.obterLinha(), "atributo da classe");
            }
        }

        // 2.o passo: verificar inicializacoes de atributos e analisar os metodos.
        for (NoAST membro : noClasse.obterFilhos()) {
            if ("atributo".equals(membro.obterNome())) {
                verificarInicializacao(membro, "atributo da classe");
            } else if ("metodo".equals(membro.obterNome())) {
                analisarMetodo(membro);
            }
        }

        sairEscopo();
    }

    private void analisarMetodo(NoAST noMetodo) {
        entrarEscopo(); // escopo do metodo
        String retornoAnterior = tipoRetornoAtual;
        tipoRetornoAtual = noMetodo.obterTipo();

        // Os parametros aparecem como primeiros filhos do no do metodo.
        for (NoAST filho : noMetodo.obterFilhos()) {
            if ("parametro".equals(filho.obterNome())) {
                declararNoEscopo(filho.obterLexema(), filho.obterTipo(), filho.obterLinha(), "parametro do metodo");
            }
        }

        // Os restantes filhos sao os comandos do corpo.
        for (NoAST filho : noMetodo.obterFilhos()) {
            if (!"parametro".equals(filho.obterNome())) {
                analisarComando(filho);
            }
        }

        tipoRetornoAtual = retornoAnterior;
        sairEscopo();
    }

    // ==================== COMANDOS ====================

    private void analisarComando(NoAST no) {
        if (no == null) {
            return;
        }
        if ("erro".equals(no.obterNome())) {
            return;
        }
        String tipo = no.obterNome();
        if ("bloco".equals(tipo)) {
            entrarEscopo();
            for (NoAST filho : no.obterFilhos()) {
                analisarComando(filho);
            }
            sairEscopo();
        } else if ("declaracoes".equals(tipo)) {
            for (NoAST filho : no.obterFilhos()) {
                declararLocal(filho);
            }
        } else if ("declaracaoLocal".equals(tipo)) {
            declararLocal(no);
        } else if ("comandoExpressao".equals(tipo)) {
            tipoDe(no.obterFilho(0));
        } else if ("se".equals(tipo)) {
            verificarCondicao(no.obterFilho(0), "if");
            analisarComando(no.obterFilho(1));
            if (no.quantidadeFilhos() > 2) {
                analisarComando(no.obterFilho(2));
            }
        } else if ("enquanto".equals(tipo)) {
            verificarCondicao(no.obterFilho(0), "while");
            analisarComando(no.obterFilho(1));
        } else if ("para".equals(tipo)) {
            entrarEscopo(); // o for tem o seu proprio escopo (init + corpo)
            analisarComando(no.obterFilho(0));           // inicializador
            if (!ehVazio(no.obterFilho(1))) {
                verificarCondicao(no.obterFilho(1), "for");
            }
            if (!ehVazio(no.obterFilho(2))) {
                tipoDe(no.obterFilho(2));                 // atualizacao
            }
            analisarComando(no.obterFilho(3));           // corpo
            sairEscopo();
        } else if ("retorno".equals(tipo)) {
            if (no.quantidadeFilhos() > 0) {
                String tipoExpr = tipoDe(no.obterFilho(0));
                if (!compativelAtribuicao(tipoRetornoAtual, tipoExpr)) {
                    registar(no.obterLinha(), "return", "incompatibilidade de tipos",
                            "nao e possivel retornar '" + tipoExpr + "' de um metodo '" + tipoRetornoAtual + "'", "comando return");
                }
            }
        } else if ("vazio".equals(tipo) || "break".equals(tipo) || "continue".equals(tipo)) {
            // nada a verificar
        } else {
            // qualquer outro no que seja uma expressao solta
            tipoDe(no);
        }
    }

    private boolean ehVazio(NoAST no) {
        return no == null || "vazio".equals(no.obterNome());
    }

    private void declararLocal(NoAST no) {
        String tipoDeclarado = no.obterTipo();
        // Regra 2: dupla declaracao no mesmo escopo (verificada dentro do metodo).
        if (declaradoNoEscopoAtual(no.obterLexema())) {
            registar(no.obterLinha(), no.obterLexema(), "variavel declarada duas vezes",
                    "o identificador '" + no.obterLexema() + "' ja foi declarado neste escopo", "declaracao de variavel");
        } else {
            escopos.peek().put(no.obterLexema(), tipoDeclarado);
        }
        verificarInicializacao(no, "declaracao de variavel");
    }

    private void verificarInicializacao(NoAST noDeclaracao, String contexto) {
        if (noDeclaracao.quantidadeFilhos() == 0) {
            return;
        }
        if (subarvoreInvalida(noDeclaracao)) {
            return;
        }
        NoAST inicializador = noDeclaracao.obterFilho(0);
        String tipoDeclarado = noDeclaracao.obterTipo();

        if ("inicializadorArray".equals(inicializador.obterNome())) {
            String tipoElemento = tipoElementoDe(tipoDeclarado);
            for (NoAST elemento : inicializador.obterFilhos()) {
                String tipoValor = tipoDe(elemento);
                if (!compativelAtribuicao(tipoElemento, tipoValor)) {
                    registar(elemento.obterLinha(), noDeclaracao.obterLexema(), "atribuicao incompativel",
                            "elemento do tipo '" + tipoValor + "' incompativel com array de '" + tipoElemento + "'", contexto);
                }
            }
            return;
        }

        String tipoValor = tipoDe(inicializador);
        if (!compativelAtribuicao(tipoDeclarado, tipoValor)) {
            registar(noDeclaracao.obterLinha(), noDeclaracao.obterLexema(), "atribuicao incompativel",
                    "nao e possivel atribuir '" + tipoValor + "' a variavel do tipo '" + tipoDeclarado + "'", contexto);
        }
    }

    private void verificarCondicao(NoAST condicao, String contexto) {
        String tipo = tipoDe(condicao);
        if (!ehDesconhecido(tipo) && !"boolean".equals(tipo)) {
            registar(condicao.obterLinha(), "", "condicao invalida",
                    "a condicao de '" + contexto + "' deve ser boolean, mas e '" + tipo + "'", "estrutura de controlo " + contexto);
        }
    }

    // ==================== EXPRESSOES / INFERENCIA DE TIPOS ====================

    /**
     * Infere o tipo de uma expressao, regista-o no proprio no (para inspeccao)
     * e devolve-o. Durante o processo detecta usos de variaveis nao declaradas,
     * atribuicoes/operacoes incompativeis e argumentos de metodos invalidos.
     */
    private String tipoDe(NoAST no) {
        if (no == null) {
            return DESCONHECIDO;
        }
        if (subarvoreInvalida(no)) {
            no.definirTipo(DESCONHECIDO);
            return DESCONHECIDO;
        }
        String tipo = inferir(no);
        no.definirTipo(tipo);
        return tipo;
    }

    private boolean subarvoreInvalida(NoAST no) {
        if (no == null) {
            return true;
        }
        if ("erro".equals(no.obterNome())) {
            return true;
        }
        for (NoAST filho : no.obterFilhos()) {
            if (subarvoreInvalida(filho)) {
                return true;
            }
        }
        return false;
    }

    private String inferir(NoAST no) {
        String nome = no.obterNome();
        switch (nome) {
            case "literalInt": return "int";
            case "literalReal": return "double";
            case "literalString": return "String";
            case "literalChar": return "char";
            case "literalBool": return "boolean";
            case "literalNull": return "null";
            case "identificador": return tipoDeIdentificador(no);
            case "this":
            case "super": return DESCONHECIDO;
            case "binario": return tipoDeBinario(no);
            case "unario": return tipoDeUnario(no);
            case "atribuicao": return tipoDeAtribuicao(no);
            case "ternario": return tipoDeTernario(no);
            case "acessoArray": return tipoDeAcessoArray(no);
            case "acessoMembro":
                tipoDe(no.obterFilho(0)); // avalia o alvo para detectar nao declarados
                return DESCONHECIDO;
            case "chamada": return tipoDeChamada(no);
            case "novoObjeto": return no.obterLexema();
            case "novoArray": return tipoDeNovoArray(no);
            case "posIncremento": return tipoDe(no.obterFilho(0));
            case "inicializadorArray":
            case "erro":
            default: return DESCONHECIDO;
        }
    }

    private String tipoDeIdentificador(NoAST no) {
        String nome = no.obterLexema();
        String tipo = resolverTipo(nome);
        if (tipo != null) {
            return tipo;
        }
        if (nomesDeClasses.contains(nome)) {
            return nome; // referencia a uma classe (ex.: chamada estatica)
        }
        registar(no.obterLinha(), nome, "variavel nao declarada",
                "o identificador '" + nome + "' nao foi declarado", "uso de identificador");
        return DESCONHECIDO;
    }

    private String tipoDeBinario(NoAST no) {
        String operador = no.obterLexema();
        String a = tipoDe(no.obterFilho(0));
        String b = tipoDe(no.obterFilho(1));

        if (ehRelacional(operador)) {
            if (!ehDesconhecido(a) && !ehDesconhecido(b) && !(ehNumerico(a) && ehNumerico(b))) {
                registar(no.obterLinha(), operador, "operacao invalida",
                        "operador relacional '" + operador + "' exige operandos numericos ('" + a + "' e '" + b + "')", "expressao");
            }
            return "boolean";
        }
        if (ehIgualdade(operador)) {
            return "boolean";
        }
        if (ehLogico(operador)) {
            if (!ehDesconhecido(a) && !ehDesconhecido(b) && !("boolean".equals(a) && "boolean".equals(b))) {
                registar(no.obterLinha(), operador, "operacao invalida",
                        "operador logico '" + operador + "' exige operandos boolean ('" + a + "' e '" + b + "')", "expressao");
            }
            return "boolean";
        }
        if (ehBitwise(operador)) {
            if ("boolean".equals(a) && "boolean".equals(b)) {
                return "boolean";
            }
            if (ehNumerico(a) && ehNumerico(b)) {
                return maisAmplo(a, b);
            }
            return ehDesconhecido(a) || ehDesconhecido(b) ? DESCONHECIDO : "int";
        }
        // aritmeticos: + - * / %
        if ("+".equals(operador) && ("String".equals(a) || "String".equals(b))) {
            return "String"; // concatenacao
        }
        if (ehDesconhecido(a) || ehDesconhecido(b)) {
            return DESCONHECIDO;
        }
        if (ehNumerico(a) && ehNumerico(b)) {
            return maisAmplo(a, b);
        }
        registar(no.obterLinha(), operador, "operacao invalida",
                "operador '" + operador + "' nao se aplica a '" + a + "' e '" + b + "'", "expressao");
        return DESCONHECIDO;
    }

    private String tipoDeUnario(NoAST no) {
        String operador = no.obterLexema();
        String tipo = tipoDe(no.obterFilho(0));
        if ("!".equals(operador)) {
            if (!ehDesconhecido(tipo) && !"boolean".equals(tipo)) {
                registar(no.obterLinha(), operador, "operacao invalida",
                        "operador '!' exige um operando boolean, mas recebeu '" + tipo + "'", "expressao");
            }
            return "boolean";
        }
        // + - ++ --
        if (!ehDesconhecido(tipo) && !ehNumerico(tipo)) {
            registar(no.obterLinha(), operador, "operacao invalida",
                    "operador '" + operador + "' exige um operando numerico, mas recebeu '" + tipo + "'", "expressao");
            return DESCONHECIDO;
        }
        return tipo;
    }

    private String tipoDeAtribuicao(NoAST no) {
        String destino = tipoDe(no.obterFilho(0));
        String origem = tipoDe(no.obterFilho(1));
        if (!compativelAtribuicao(destino, origem)) {
            registar(no.obterLinha(), no.obterFilho(0).obterLexema(), "atribuicao incompativel",
                    "nao e possivel atribuir '" + origem + "' a um destino do tipo '" + destino + "'", "atribuicao");
        }
        return destino;
    }

    private String tipoDeTernario(NoAST no) {
        verificarCondicao(no.obterFilho(0), "operador ternario");
        String entao = tipoDe(no.obterFilho(1));
        String senao = tipoDe(no.obterFilho(2));
        if (ehNumerico(entao) && ehNumerico(senao)) {
            return maisAmplo(entao, senao);
        }
        if (entao != null && entao.equals(senao)) {
            return entao;
        }
        return ehDesconhecido(entao) ? senao : entao;
    }

    private String tipoDeAcessoArray(NoAST no) {
        String base = tipoDe(no.obterFilho(0));
        String indice = tipoDe(no.obterFilho(1));
        if (!ehDesconhecido(indice) && !ehNumerico(indice)) {
            registar(no.obterLinha(), "[]", "indice invalido",
                    "o indice de um array deve ser numerico, mas e '" + indice + "'", "acesso a array");
        }
        return tipoElementoDe(base);
    }

    private String tipoDeNovoArray(NoAST no) {
        for (NoAST dimensao : no.obterFilhos()) {
            tipoDe(dimensao);
        }
        return no.obterLexema() + "[]";
    }

    private String tipoDeChamada(NoAST no) {
        String nome = no.obterLexema();
        NoAST alvo = no.obterFilho(0);
        List<NoAST> argumentos = argumentosDe(no);

        if ("identificador".equals(alvo.obterNome())) {
            // chamada simples: nome(args) -> tem de ser um metodo declarado
            Simbolo metodo = tabela.procurarMetodo(nome);
            if (metodo == null) {
                registar(no.obterLinha(), nome, "metodo nao declarado",
                        "o metodo '" + nome + "' nao foi declarado", "chamada de metodo");
                avaliarArgumentos(argumentos);
                return DESCONHECIDO;
            }
            validarArgumentos(metodo, argumentos, no);
            return metodo.obterTipoRetorno();
        }

        if ("acessoMembro".equals(alvo.obterNome())) {
            // chamada por objecto: obj.metodo(args). Avalia o objecto para
            // detectar nao declarados; valida a assinatura se o metodo for nosso.
            tipoDe(alvo.obterFilho(0));
            Simbolo metodo = tabela.procurarMetodo(nome);
            if (metodo != null) {
                validarArgumentos(metodo, argumentos, no);
                return metodo.obterTipoRetorno();
            }
            avaliarArgumentos(argumentos);
            return DESCONHECIDO;
        }

        tipoDe(alvo);
        avaliarArgumentos(argumentos);
        return DESCONHECIDO;
    }

    private List<NoAST> argumentosDe(NoAST chamada) {
        List<NoAST> argumentos = new ArrayList<NoAST>();
        for (int i = 1; i < chamada.quantidadeFilhos(); i++) {
            argumentos.add(chamada.obterFilho(i));
        }
        return argumentos;
    }

    private void avaliarArgumentos(List<NoAST> argumentos) {
        for (NoAST argumento : argumentos) {
            tipoDe(argumento);
        }
    }

    /** Regra 5: verifica numero, tipo e ordem dos argumentos de uma chamada. */
    private void validarArgumentos(Simbolo metodo, List<NoAST> argumentos, NoAST chamada) {
        List<String> parametros = metodo.obterParametros();
        if (parametros.size() != argumentos.size()) {
            registar(chamada.obterLinha(), metodo.obterLexema(), "numero de argumentos invalido",
                    "o metodo '" + metodo.obterAssinatura() + "' espera " + parametros.size()
                            + " argumento(s), mas recebeu " + argumentos.size(), "chamada de metodo");
        }

        int comuns = Math.min(parametros.size(), argumentos.size());
        for (int i = 0; i < comuns; i++) {
            String tipoParametro = tipoDoParametro(parametros.get(i));
            String tipoArgumento = tipoDe(argumentos.get(i));
            if (!compativelAtribuicao(tipoParametro, tipoArgumento)) {
                registar(argumentos.get(i).obterLinha(), metodo.obterLexema(), "argumento incompativel",
                        "argumento " + (i + 1) + " do tipo '" + tipoArgumento + "' incompativel com o parametro '"
                                + tipoParametro + "' de '" + metodo.obterAssinatura() + "'", "chamada de metodo");
            }
        }
        // avalia argumentos excedentes (para detectar nao declarados dentro deles)
        for (int i = comuns; i < argumentos.size(); i++) {
            tipoDe(argumentos.get(i));
        }
    }

    private String tipoDoParametro(String parametro) {
        int espaco = parametro.lastIndexOf(' ');
        return espaco > 0 ? parametro.substring(0, espaco) : parametro;
    }

    // ==================== REGRAS DE TIPOS ====================

    private boolean compativelAtribuicao(String destino, String origem) {
        if (ehDesconhecido(destino) || ehDesconhecido(origem)) {
            return true; // suprime erros em cascata
        }
        if (destino.equals(origem)) {
            return true;
        }
        if ("null".equals(origem)) {
            return ehReferencia(destino);
        }
        if (ehNumerico(destino) && ehNumerico(origem)) {
            return NUMERICOS.indexOf(origem) <= NUMERICOS.indexOf(destino); // alargamento
        }
        return false;
    }

    private boolean ehReferencia(String tipo) {
        return "String".equals(tipo) || tipo.endsWith("[]") || nomesDeClasses.contains(tipo);
    }

    private boolean ehNumerico(String tipo) {
        return NUMERICOS.contains(tipo);
    }

    private String maisAmplo(String a, String b) {
        return NUMERICOS.indexOf(a) >= NUMERICOS.indexOf(b) ? a : b;
    }

    private boolean ehDesconhecido(String tipo) {
        return tipo == null || DESCONHECIDO.equals(tipo) || "erro".equals(tipo) || "void".equals(tipo);
    }

    private String tipoElementoDe(String tipo) {
        if (tipo != null && tipo.endsWith("[]")) {
            return tipo.substring(0, tipo.length() - 2);
        }
        return DESCONHECIDO;
    }

    private boolean ehRelacional(String op) {
        return "<".equals(op) || ">".equals(op) || "<=".equals(op) || ">=".equals(op);
    }

    private boolean ehIgualdade(String op) {
        return "==".equals(op) || "!=".equals(op);
    }

    private boolean ehLogico(String op) {
        return "&&".equals(op) || "||".equals(op);
    }

    private boolean ehBitwise(String op) {
        return "&".equals(op) || "|".equals(op) || "^".equals(op);
    }

    private void registar(int linha, String lexema, String tipoErro, String descricao, String contexto) {
        erros.add(new ErroSemantico(linha, lexema, tipoErro, descricao, contexto));
    }
}
