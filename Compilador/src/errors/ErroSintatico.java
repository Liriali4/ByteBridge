package errors;

/**
 * Representa um erro detectado durante a analise sintatica (Fase 2).
 *
 * Cada erro guarda:
 *   - linha    -> a linha onde o erro REALMENTE ocorre. Para simbolos em falta
 *                 (";", ")", "]", "}") esta e a linha do ultimo token valido lido,
 *                 e nao a linha onde a sincronizacao terminou;
 *   - esperado -> o que a gramatica esperava naquele ponto;
 *   - recebido -> o token efectivamente encontrado;
 *   - contexto -> a regra/gramatica que estava a ser analisada;
 *   - apos     -> (opcional) o lexema apos o qual faltou o simbolo esperado,
 *                 usado para dar mais precisao a mensagem.
 */
public class ErroSintatico {
    private final int linha;
    private final String esperado;
    private final String recebido;
    private final String contexto;
    private final String apos;

    public ErroSintatico(int linha, String esperado, String recebido, String contexto) {
        this(linha, esperado, recebido, contexto, "");
    }

    public ErroSintatico(int linha, String esperado, String recebido, String contexto, String apos) {
        this.linha = linha;
        this.esperado = esperado;
        this.recebido = recebido;
        this.contexto = contexto;
        this.apos = apos == null ? "" : apos;
    }

    public int obterLinha() {
        return linha;
    }

    @Override
    public String toString() {
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("Erro na linha ").append(linha)
                .append(" [").append(contexto).append("]: esperado ").append(esperado);
        if (!apos.isEmpty()) {
            mensagem.append(" apos '").append(apos).append("'");
        }
        mensagem.append(", mas encontrado ").append(recebido);
        return mensagem.toString();
    }
}
