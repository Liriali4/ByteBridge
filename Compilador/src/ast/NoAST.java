package ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoAST {
    private final String nome;
    private final List<NoAST> filhos = new ArrayList<NoAST>();

    public NoAST(String nome) {
        this.nome = nome;
    }

    public void adicionarFilho(NoAST filho) {
        if (filho != null) {
            filhos.add(filho);
        }
    }

    public String obterNome() {
        return nome;
    }

    public List<NoAST> obterFilhos() {
        return Collections.unmodifiableList(filhos);
    }
}
