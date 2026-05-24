package errors;

public class ErroSintatico {
    private final int linha;
    private final String esperado;
    private final String recebido;
    private final String contexto;

    public ErroSintatico(int linha, String esperado, String recebido, String contexto) {
        this.linha = linha;
        this.esperado = esperado;
        this.recebido = recebido;
        this.contexto = contexto;
    }

    public int obterLinha() {
        return linha;
    }

    @Override
    public String toString() {
        return "Erro Sintatico na linha " + linha + " [" + contexto + "]: esperado "
                + esperado + ", mas encontrado " + recebido;
    }
}
