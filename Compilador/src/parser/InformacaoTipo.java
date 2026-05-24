package parser;

class InformacaoTipo {
    private final String nome;
    private final int dimensoes;

    InformacaoTipo(String nome, int dimensoes) {
        this.nome = nome;
        this.dimensoes = dimensoes;
    }

    String obterNome() {
        return nome;
    }

    int obterDimensoes() {
        return dimensoes;
    }

    String comoTexto() {
        StringBuilder texto = new StringBuilder(nome);
        for (int i = 0; i < dimensoes; i++) {
            texto.append("[]");
        }
        return texto.toString();
    }
}
