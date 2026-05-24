package symbols;

import java.util.LinkedHashMap;
import java.util.Map;

public class Escopo {
    private final String nome;
    private final String categoria;
    private final Escopo pai;
    private final Map<String, Simbolo> simbolos = new LinkedHashMap<String, Simbolo>();

    public Escopo(String nome, String categoria, Escopo pai) {
        this.nome = nome;
        this.categoria = categoria;
        this.pai = pai;
    }

    public boolean declarar(Simbolo Simbolo) {
        if (simbolos.containsKey(Simbolo.obterLexema())) {
            return false;
        }
        simbolos.put(Simbolo.obterLexema(), Simbolo);
        return true;
    }

    public Simbolo resolver(String lexema) {
        Simbolo local = simbolos.get(lexema);
        if (local != null) {
            return local;
        }
        return pai == null ? null : pai.resolver(lexema);
    }

    public Escopo obterPai() {
        return pai;
    }

    public String obterNome() {
        return nome;
    }

    public String obterCategoria() {
        return categoria;
    }

    public Map<String, Simbolo> obterSimbolos() {
        return simbolos;
    }
}
