package symbols;

import java.util.ArrayList;
import java.util.List;

public class TabelaSimbolos {
    private final List<Escopo> todosEscopos = new ArrayList<Escopo>();
    private Escopo escopoAtual;
    private int proximoEndereco = 0;

    public TabelaSimbolos() {
        entrarEscopo("global", "global");
    }

    public void entrarEscopo(String nome, String categoria) {
        escopoAtual = new Escopo(nome, categoria, escopoAtual);
        todosEscopos.add(escopoAtual);
    }

    public void sairEscopo() {
        if (escopoAtual != null && escopoAtual.obterPai() != null) {
            escopoAtual = escopoAtual.obterPai();
        }
    }

    public boolean declarar(Simbolo simbolo) {
        if (escopoAtual == null) {
            return false;
        }
        if (escopoAtual.contemNoEscopoAtual(simbolo.obterLexema())) {
            return false;
        }

        int tamanho = tamanhoParaDeclaracao(simbolo);
        simbolo.definirEscopo(escopoAtual.obterNome());
        simbolo.definirTamanho(tamanho);
        if (tamanho > 0) {
            simbolo.definirEndereco(proximoEndereco);
            proximoEndereco += tamanho;
        } else {
            simbolo.definirEndereco(-1);
        }
        return escopoAtual.declarar(simbolo);
    }

    public Simbolo resolver(String lexema) {
        return escopoAtual == null ? null : escopoAtual.resolver(lexema);
    }

    // ==================== CONSULTAS PARA A ANALISE SEMANTICA ====================

    /** A variavel/simbolo existe, visivel a partir do escopo atual? */
    public boolean existe(String lexema) {
        return resolver(lexema) != null;
    }

    /** O simbolo esta declarado exactamente no escopo actual (nao herdado)? */
    public boolean declaradoNoEscopoAtual(String lexema) {
        return escopoAtual != null && escopoAtual.contemNoEscopoAtual(lexema);
    }

    /** O simbolo esta declarado nalgum escopo superior (mas nao no atual)? */
    public boolean declaradoEmEscopoSuperior(String lexema) {
        if (escopoAtual == null || escopoAtual.contemNoEscopoAtual(lexema)) {
            return false;
        }
        return escopoAtual.obterPai() != null && escopoAtual.obterPai().resolver(lexema) != null;
    }

    /** Tipo de dado de um simbolo visivel, ou null se nao existir. */
    public String tipoDe(String lexema) {
        Simbolo simbolo = resolver(lexema);
        return simbolo == null ? null : simbolo.obterTipoDado();
    }

    /** Um simbolo visivel esta marcado como inicializado? */
    public boolean estaInicializada(String lexema) {
        Simbolo simbolo = resolver(lexema);
        return simbolo != null && simbolo.estaInicializado();
    }

    /**
     * Procura, em TODOS os escopos, um metodo com o nome dado. Ao contrario de
     * resolver(), nao depende do escopo actual -- util na Fase 3, onde a analise
     * ja percorre a tabela toda e o escopo actual ja voltou ao global.
     */
    public Simbolo procurarMetodo(String nome) {
        for (Escopo escopo : todosEscopos) {
            for (Simbolo simbolo : escopo.obterSimbolos().values()) {
                if ("metodo".equals(simbolo.obterCategoria()) && simbolo.obterLexema().equals(nome)) {
                    return simbolo;
                }
            }
        }
        return null;
    }

    /** Existe algum metodo declarado com este nome? */
    public boolean metodoExiste(String nome) {
        return procurarMetodo(nome) != null;
    }

    public List<Escopo> obterEscopos() {
        return todosEscopos;
    }

    public String nomeEscopoAtual() {
        return escopoAtual == null ? "global" : escopoAtual.obterNome();
    }

    public String categoriaEscopoAtual() {
        return escopoAtual == null ? "global" : escopoAtual.obterCategoria();
    }

    public int tamanhoSimbolo(String tipo) {
        if (tipo == null) {
            return 4;
        }
        String base = tipo.replace("[]", "");
        if ("double".equals(base) || "long".equals(base)) {
            return 8;
        }
        if ("boolean".equals(base)) {
            return 1;
        }
        if ("char".equals(base)) {
            return 2;
        }
        if ("float".equals(base) || "int".equals(base)) {
            return 4;
        }
        return 4;
    }

    private int tamanhoParaDeclaracao(Simbolo simbolo) {
        if ("metodo".equals(simbolo.obterCategoria()) || "classe".equals(simbolo.obterCategoria())) {
            return 0;
        }
        if (simbolo.obterTamanho() > 0) {
            return simbolo.obterTamanho();
        }
        return tamanhoSimbolo(simbolo.obterTipoDado());
    }

    public void imprimir() {
        System.out.println("\nTABELA DE SIMBOLOS");
        System.out.println("============");
        for (Escopo escopo : todosEscopos) {
            System.out.println("Escopo: " + escopo.obterNome() + " (" + escopo.obterCategoria() + ")");
            for (Simbolo simbolo : escopo.obterSimbolos().values()) {
                System.out.println("  " + simbolo);
            }
        }
    }
}
