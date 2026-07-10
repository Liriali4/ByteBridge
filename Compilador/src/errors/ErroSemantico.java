package errors;

/**
 * Representa um erro detectado durante a analise semantica (Fase 3).
 *
 * E uma classe totalmente separada de ErroLexico e ErroSintatico, para que
 * cada fase do compilador tenha o seu proprio tipo de erro e nunca haja
 * confusao entre as categorias.
 *
 * Cada erro semantico informa, conforme exigido pelo enunciado:
 *   - linha     -> onde o problema ocorre no codigo fonte;
 *   - lexema    -> o simbolo/identificador envolvido;
 *   - tipoErro  -> a categoria do erro (ex.: "variavel nao declarada");
 *   - descricao -> explicacao clara do que esta errado;
 *   - contexto  -> a construcao onde o erro foi detectado.
 */
public class ErroSemantico {
    private final int linha;
    private final String lexema;
    private final String tipoErro;
    private final String descricao;
    private final String contexto;

    public ErroSemantico(int linha, String lexema, String tipoErro, String descricao, String contexto) {
        this.linha = linha;
        this.lexema = lexema == null ? "" : lexema;
        this.tipoErro = tipoErro;
        this.descricao = descricao;
        this.contexto = contexto;
    }

    public int obterLinha() {
        return linha;
    }

    public String obterTipoErro() {
        return tipoErro;
    }

    @Override
    public String toString() {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("Erro na linha ").append(linha)
                .append(" [").append(tipoErro).append("]");
        if (!lexema.isEmpty()) {
            mensagem.append(" em '").append(lexema).append("'");
        }
        mensagem.append(": ").append(descricao)
                .append(" (contexto: ").append(contexto).append(")");
        return mensagem.toString();
    }
}
