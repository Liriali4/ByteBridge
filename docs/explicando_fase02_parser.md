# Fase 2 — Analisador Sintático

## Objetivo da Fase

O Analisador Sintático verifica se a sequência de tokens produzida pelo lexer obedece à gramática da linguagem. Usa a estratégia **descendente recursiva** com **lookahead de 1 token (LL(1))**.

Além de verificar a estrutura, o parser constrói a **Tabela de Símbolos** com informação sobre classes, métodos, parâmetros e variáveis.

---

## Classes Envolvidas

| Classe | Pacote | Ficheiro |
|---|---|---|
| `AnalisadorSintatico` | `parser` | `AnalisadorSintatico.java` |
| `InformacaoTipo` | `parser` | `InformacaoTipo.java` |
| `ErroSintatico` | `errors` | `ErroSintatico.java` |
| `ExcecaoSintatica` | `errors` | `ExcecaoSintatica.java` |
| `Simbolo` | `symbols` | `Simbolo.java` |
| `TabelaSimbolos` | `symbols` | `TabelaSimbolos.java` |
| `Escopo` | `symbols` | `Escopo.java` |
| `NoAST` | `ast` | `NoAST.java` |

---

## Atributos do AnalisadorSintatico

```
AnalisadorLexico lexer          → referência ao lexer para pedir tokens
TabelaSimbolos tabelaSimbolos   → tabela de símbolos construída durante a análise
List<ErroSintatico> listaErros  → lista de todos os erros encontrados
Token tokenAtual                → token que está a ser analisado agora
Token tokenSeguinte             → próximo token (lookahead de 1)
int contadorBlocos              → contador para nomear escopos de blocos anónimos
String ultimoErro               → chave do último erro registado (evita duplicados)
```

---

## Conjuntos de Sincronização

Definidos como constantes estáticas. Usados no modo pânico.

```java
SINCRONIZACAO_DECLARACAO = { ";", "}", "class", "public", "private",
    "protected", "static", "final", "void", "int", "double",
    "boolean", "char", "float", "long", "String" }

SINCRONIZACAO_BLOCO = { "}", "{", "if", "while", "for", "return",
    "break", "continue", "int", "double", "boolean", "char",
    "float", "long", "String" }

SINCRONIZACAO_EXPRESSAO = { ";", ")", "]", ",", "}", ":", "{" }

SINCRONIZACAO_ESTRUTURA = { ")", "{", "}", ";", "else" }
```

---

## Construtor

```java
public AnalisadorSintatico(AnalisadorLexico lexer)
```

- Guarda referência ao lexer
- Chama `lerToken()` duas vezes para preencher `tokenAtual` e `tokenSeguinte`
- Inicializa a tabela de símbolos (que cria automaticamente o escopo global)

---

## Estratégia LL(1) com Dois Tokens

O parser mantém sempre dois tokens em memória:

```
tokenAtual   → o token que está a ser processado agora
tokenSeguinte → o próximo token (lookahead)
```

Isto permite ao parser tomar decisões sem consumir tokens desnecessariamente. Por exemplo, para distinguir declaração de variável de chamada de método:

```
int x = 5;          → tokenAtual="int", tokenSeguinte="x"  → declaração
minhaFuncao(x);     → tokenAtual=IDENTIFICADOR, tokenSeguinte=IDENTIFICADOR → declaração
minhaFuncao(x);     → tokenAtual=IDENTIFICADOR, tokenSeguinte="(" → chamada
```

---

## Métodos de Controlo de Tokens

### lerToken()

```java
private Token lerToken()
```

- Chama `lexer.analex()` e regista a linha com `lexer.getLinhaAtual()`
- **Ignora automaticamente comentários** (TOKEN_COMENTARIO): chama analex() de novo
- Se receber TOKEN_ERRO, regista um ErroSintatico e devolve o token de erro
- Chamado apenas por `avancar()` e pelo construtor

### avancar()

```java
private void avancar()
```

- Move a janela: `tokenAtual = tokenSeguinte`
- Pede novo token ao lexer: `tokenSeguinte = lerToken()`
- Chamado por todos os métodos que consomem tokens

### ehLexema(String lexema)

```java
private boolean ehLexema(String lexema)
```

- Retorna `true` se `tokenAtual.lexema.equals(lexema)`
- Não consome o token

### ehLexemaSeguinte(String lexema)

```java
private boolean ehLexemaSeguinte(String lexema)
```

- Verifica o `tokenSeguinte` sem consumir
- Usado para lookahead de 2 tokens em casos específicos

### verificarLexema(String lexema)

```java
private boolean verificarLexema(String lexema)
```

- Se `tokenAtual` tem o lexema esperado, chama `avancar()` e retorna `true`
- Caso contrário, retorna `false` sem consumir
- Usado para tokens opcionais

### consumirLexema(String lexema, String contexto)

```java
private Token consumirLexema(String lexema, String contexto)
```

- Se `tokenAtual` tem o lexema esperado, consome e retorna o token
- Se não, chama `relatarErro()` e tenta recuperar:
  - Se `tokenSeguinte` tem o lexema, avança duas vezes (salta o token errado)
  - Caso contrário, retorna um token sintético `<missing>`
- Usado para tokens obrigatórios

### consumirIdentificador(String contexto)

```java
private Token consumirIdentificador(String contexto)
```

- Verifica se `tokenAtual` é um identificador (não palavra reservada)
- Se sim, consome e retorna
- Se não, regista erro, cria token sintético `<missing>` e tenta avançar

### sintetico(String lexema)

```java
private Token sintetico(String lexema)
```

- Cria um token fictício com o lexema dado e código TOKEN_IDENTIFICADOR
- Usado para recuperação de erros — permite continuar a análise mesmo sem o token esperado

---

## Gramática e Métodos Correspondentes

### Programa

```
Programa → DeclaracaoPacote DeclaracaoImportacao* DeclaracaoTipo*

analisarPrograma()
    └─ analisarDeclaracaoPacote()
    └─ loop: analisarDeclaracaoImportacao()
    └─ loop: analisarDeclaracaoTipo()
```

### Declaração de Pacote

```
DeclaracaoPacote → 'package' NomeQualificado ';'

analisarDeclaracaoPacote()
    └─ consumirLexema("package", ...)
    └─ analisarNomeQualificado(...)
    └─ consumirFimDeclaracao(...)
```

### Declaração de Importação

```
DeclaracaoImportacao → 'import' Identificador ('.' Identificador | '.' '*')* ';'

analisarDeclaracaoImportacao()
    └─ consumirLexema("import", ...)
    └─ consumirIdentificador(...)
    └─ loop: verificarLexema(".") → consumirIdentificador ou verificarLexema("*")
    └─ consumirFimDeclaracao(...)
```

### Declaração de Tipo (Classe)

```
DeclaracaoTipo → Modificadores* 'class' Identificador ('extends' Tipo)? '{' Membro* '}'

analisarDeclaracaoTipo()
    └─ analisarModificadores()
    └─ analisarDeclaracaoClasse(modificadores)

analisarDeclaracaoClasse(modificadores)
    └─ consumirLexema("class", ...)
    └─ consumirIdentificador(...)  → nomeClasse
    └─ criar Simbolo categoria="classe"
    └─ declarar(simbolo, ...)
    └─ verificarLexema("extends") → analisarTipo()
    └─ consumirLexema("{", ...)
    └─ tabelaSimbolos.entrarEscopo(nomeClasse, "classe")
    └─ loop: analisarMembroClasse()
    └─ consumirLexema("}", ...)
    └─ tabelaSimbolos.sairEscopo()
```

### Membro de Classe

```
Membro → Modificadores* Tipo Identificador ('(' Parametros ')' CorpoMetodo | (',' Identificador)* ';')

analisarMembroClasse()
    └─ analisarModificadores()
    └─ analisarTipoOuVazio()  → tipo
    └─ consumirIdentificador(...)  → nome
    └─ se '(' → analisarRestanteMetodo(...)
    └─ senão  → analisarRestanteCampo(...)
```

### Método

```
analisarRestanteMetodo(modificadores, tipo, nome)
    └─ criar Simbolo categoria="metodo"
    └─ definirTipoRetorno(tipo)
    └─ declarar(simbolo, ...)
    └─ tabelaSimbolos.entrarEscopo(nome, "metodo")
    └─ analisarListaParametros(metodo)
    └─ analisarCorpoMetodo()
    └─ tabelaSimbolos.sairEscopo()
```

### Parâmetros

```
ListaParametros → (Tipo Identificador (',' Tipo Identificador)*)? ')'

analisarListaParametros(metodo)
    └─ se não ')': analisarParametro(metodo)
    └─ loop: verificarLexema(",") → analisarParametro(metodo)
    └─ consumirLexema(")", ...)

analisarParametro(metodo)
    └─ analisarTipo()  → tipo
    └─ consumirIdentificador(...)  → nome
    └─ metodo.obterParametros().add(tipo + " " + nome)
    └─ criar Simbolo categoria="parametro", tipoVariavel="local", inicializado=true
    └─ declarar(simbolo, ...)
```

### Corpo do Método e Bloco

```
CorpoMetodo → '{' Comando* '}'
Bloco       → '{' Comando* '}'

analisarCorpoMetodo()
    └─ consumirLexema("{", ...)
    └─ loop: analisarComando()
    └─ consumirLexema("}", ...)

analisarBloco()
    └─ consumirLexema("{", ...)
    └─ tabelaSimbolos.entrarEscopo("bloco" + contador, "bloco")
    └─ loop: analisarComando()
    └─ consumirLexema("}", ...)
    └─ tabelaSimbolos.sairEscopo()
```

### Comandos

```
Comando → Bloco
        | ComandoSe
        | ComandoEnquanto
        | ComandoPara
        | ComandoRetorno
        | 'break' ';'
        | 'continue' ';'
        | DeclaracaoVariavelLocal
        | Expressao ';'

analisarComando()
    └─ '{' → analisarBloco()
    └─ 'if' → analisarComandoSe()
    └─ 'while' → analisarComandoEnquanto()
    └─ 'for' → analisarComandoPara()
    └─ 'return' → analisarComandoRetorno()
    └─ 'break' → consumir + consumirFimDeclaracao
    └─ 'continue' → consumir + consumirFimDeclaracao
    └─ ehInicioDeclaracao() → analisarDeclaracaoVariavelLocal(true)
    └─ senão → analisarExpressao() + consumir ';'
```

### Comando If

```
ComandoSe → 'if' '(' Expressao ')' Comando ('else' Comando)?

analisarComandoSe()
    └─ consumirLexema("if", ...)
    └─ consumirLexema("(", ...)
    └─ analisarExpressao()
    └─ verificarLexema(")") ou relatarErro + sincronizarEstrutura
    └─ analisarComando()
    └─ verificarLexema("else") → analisarComando()
```

### Comando While

```
ComandoEnquanto → 'while' '(' Expressao ')' Comando

analisarComandoEnquanto()
    └─ consumirLexema("while", ...)
    └─ consumirLexema("(", ...)
    └─ analisarExpressao()
    └─ verificarLexema(")") ou relatarErro + sincronizarEstrutura
    └─ analisarComando()
```

### Comando For

```
ComandoPara → 'for' '(' (DeclaracaoLocal | Expressao)? ';' Expressao? ';' Expressao? ')' Comando

analisarComandoPara()
    └─ consumirLexema("for", ...)
    └─ consumirLexema("(", ...)
    └─ inicializador: declaração ou expressão ou vazio
    └─ consumirLexema(";", ...)
    └─ condição: expressão ou vazio
    └─ consumirLexema(";", ...)
    └─ atualização: expressão ou vazio
    └─ consumirLexema(")", ...)
    └─ analisarComando()
```

### Comando Return

```
ComandoRetorno → 'return' Expressao? ';'

analisarComandoRetorno()
    └─ consumirLexema("return", ...)
    └─ se não ';': analisarExpressao()
    └─ consumirFimDeclaracao(...)
```

---

## Hierarquia de Expressões (Precedência)

A precedência é implementada pela hierarquia de chamadas. Cada nível chama o nível de maior precedência.

```
analisarExpressao()
    └─ analisarExpressaoAtribuicao()
        └─ analisarExpressaoCondicional()   (operador ternário ?)
            └─ analisarExpressaoOrLogico()  (||)
                └─ analisarExpressaoELogico()  (&&)
                    └─ analisarExpressaoOrBit()  (|)
                        └─ analisarExpressaoXorBit()  (^)
                            └─ analisarExpressaoAndBit()  (&)
                                └─ analisarExpressaoIgualdade()  (== !=)
                                    └─ analisarExpressaoRelacional()  (< > <= >=)
                                        └─ analisarExpressaoAditiva()  (+ -)
                                            └─ analisarExpressaoMultiplicativa()  (* / %)
                                                └─ analisarExpressaoUnaria()  (+ - ! ++ --)
                                                    └─ analisarExpressaoPosfixa()  ([] () . ++ --)
                                                        └─ analisarExpressaoPrimaria()
```

### Tabela de Precedência (do menor para o maior)

| Nível | Operadores | Associatividade |
|---|---|---|
| 1 | `=` `+=` `-=` `*=` `/=` `%=` `&=` `\|=` `^=` | direita |
| 2 | `?:` | direita |
| 3 | `\|\|` | esquerda |
| 4 | `&&` | esquerda |
| 5 | `\|` | esquerda |
| 6 | `^` | esquerda |
| 7 | `&` | esquerda |
| 8 | `==` `!=` | esquerda |
| 9 | `<` `>` `<=` `>=` | esquerda |
| 10 | `+` `-` | esquerda |
| 11 | `*` `/` `%` | esquerda |
| 12 | `+` `-` `!` `++` `--` (unário) | direita |
| 13 | `[]` `()` `.` `++` `--` (pós-fixo) | esquerda |

### Operadores de Atribuição Reconhecidos

```java
private boolean ehOperadorAtribuicao(String lexema) {
    return "=".equals(lexema) || "+=".equals(lexema) || "-=".equals(lexema)
        || "*=".equals(lexema) || "/=".equals(lexema) || "%=".equals(lexema)
        || "&=".equals(lexema) || "|=".equals(lexema) || "^=".equals(lexema);
}
```

### Expressão Primária

```
ExpressaoPrimaria → Literal
                  | Identificador
                  | 'this' | 'super'
                  | '(' Expressao ')'
                  | 'new' ExpressaoCriacao

analisarExpressaoPrimaria()
    └─ ehLiteral() → avancar()
    └─ ehIdentificador() → verificar na tabela de símbolos, avancar()
    └─ 'this' ou 'super' → verificarLexema
    └─ '(' → analisarExpressao() + consumir ')'
    └─ 'new' → analisarExpressaoCriacao()
    └─ senão → relatarErro + sincronizarExpressao
```

### Identificadores em Expressões (sem verificação semântica)

Em `analisarExpressaoPrimaria()`, quando encontra um identificador, o parser
apenas **cria o nó da AST** e avança:

```java
if (ehIdentificador()) {
    NoAST no = new NoExpressao("identificador", tokenAtual.lexema, tokenAtual.linha);
    avancar();
    return no;
}
```

> **Mudança importante:** a antiga verificação de "identificador não declarado"
> foi **removida do parser**. Verificar se uma variável existe é uma decisão
> *semântica*, não sintática, e passou para a Fase 3. O parser limita-se a
> registar a estrutura; quem decide se `y` está declarada é o
> `AnalisadorSemantico`. O mesmo se aplica à deteção de declarações duplicadas.

---

## Modo Pânico — Recuperação de Erros

### O que é

Quando o parser encontra um token inesperado, não pode simplesmente parar. O modo
pânico é a estratégia de recuperação que permite continuar a análise e encontrar
o **maior número possível de erros úteis** no mesmo ficheiro.

### Princípio central: registar o erro ANTES de sincronizar

O erro é **sempre registado antes** de a recuperação começar. Assim, a linha
apresentada é a do ponto onde o erro realmente ocorreu, e nunca a linha onde a
sincronização terminou.

Para **símbolos terminadores em falta** (`;`, `)`, `]`, `}`, `:`), a linha usada é
a do **último token válido consumido** — o fim da construção — e não a do token
onde a análise parou. Isto corrige o problema em que um `;` esquecido apontava
para linhas seguintes:

```java
private void relatarSimboloEmFalta(String esperado, String contexto) {
    String semAspas = esperado.replace("'", "");
    if (TERMINADORES.contains(semAspas)) {
        // linha do fim da construção (ex.: a linha do 'x' em "int x")
        registrarErro(ultimoTokenConsumido.linha, esperado,
                      descrever(tokenAtual), contexto, ultimoTokenConsumido.lexema);
    } else {
        registrarErro(tokenAtual.linha, esperado, descrever(tokenAtual), contexto, "");
    }
}
```

Exemplo: `int x` sem `;` (o `public` vem só na linha seguinte) produz agora

```
Erro na linha 2 [declaracao de atributo]: esperado ';' apos 'x', mas encontrado 'public' (PUBLIC)
```

— a linha **2** (do `x`), e não a linha do `public`.

### Fluxo do Modo Pânico

```
Token inesperado encontrado
        │
        ▼
relatarErro / relatarSimboloEmFalta   ← REGISTA primeiro (linha correta)
        │
        ▼
sincronizarXxx()                       ← só DEPOIS recupera
    └─ avancarAte(conjunto do contexto)
        │
        ▼
garantirProgresso(marca)               ← evita ciclos infinitos
        │
        ▼
Análise continua a partir do ponto seguro
```

### Sincronização específica por contexto

A sincronização genérica foi eliminada. Existe agora um método por contexto,
cada um com o seu próprio conjunto de tokens de paragem. `avancarAte(Set)` é
apenas o mecanismo de baixo nível (avançar até um token de paragem); a estratégia
é sempre escolhida pelo contexto:

| Método | Conjunto | Para em |
|--------|----------|---------|
| `sincronizarClasse()` | `SINCRONIZACAO_CLASSE` | `class` (ou EOF) |
| `sincronizarMetodo()` | `SINCRONIZACAO_MEMBRO` | `}` ou início de membro |
| `sincronizarDeclaracao()` | `SINCRONIZACAO_DECLARACAO` | `;` `}` ou início de declaração (consome `;`) |
| `sincronizarBloco()` | `SINCRONIZACAO_BLOCO` | `}` ou início de comando |
| `sincronizarComando()` | `SINCRONIZACAO_BLOCO` | idem (consome `;`) |
| `sincronizarExpressao()` | `SINCRONIZACAO_EXPRESSAO` | `;` `)` `]` `,` `}` `:` |
| `sincronizarParametros()` | `SINCRONIZACAO_PARAMETROS` | `)` `,` `{` |
| `sincronizarEstrutura()` | `SINCRONIZACAO_ESTRUTURA` | `)` `{` `}` `;` `else` |

### Prevenção de ciclos infinitos

Cada ciclo de análise (`while` sobre tipos, membros, comandos) verifica se
consumiu pelo menos um token; caso contrário, força um avanço:

```java
private void garantirProgresso(int marcaInicial) {
    if (totalAvancos == marcaInicial && !fim()) {
        avancar();
    }
}
```

Sem esta guarda, um token que pertença ao conjunto de sincronização mas não seja
consumido pela regra poderia originar um ciclo infinito (o parser voltaria a
tentar a mesma regra sobre o mesmo token, indefinidamente).

### Prevenção de Erros Duplicados

```java
private void registrarErro(int linha, String esperado, String recebido, String contexto, String apos) {
    String chave = linha + "|" + esperado + "|" + recebido + "|" + contexto;
    if (!chave.equals(ultimoErro)) {
        listaErros.add(new ErroSintatico(linha, esperado, recebido, contexto, apos));
        ultimoErro = chave;
    }
}
```

A chave combina linha, esperado, recebido e contexto. Se o mesmo erro seria
registado duas vezes (por exemplo, por chamadas recursivas), é ignorado.

---

## Construção da AST

Além de verificar a gramática e preencher a tabela de símbolos, o parser agora
**constrói uma árvore sintática real** (`ast.NoAST`). Cada método de análise de
expressão/comando devolve o nó correspondente, que o método chamador liga como
filho. Por exemplo, os níveis de precedência constroem árvores binárias
associativas à esquerda:

```java
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
```

Esta árvore é o que a **Fase 3** percorre. Ver `explicando_fase03_semantico.md`.

---

## Classe ErroSintatico

```java
public class ErroSintatico {
    private final int linha;
    private final String esperado;
    private final String recebido;
    private final String contexto;
}
```

### toString()

```
"Erro Sintatico na linha 5 [declaracao de variavel local]: esperado ';', mas encontrado 'x' (IDENTIFICADOR)"
```

---

## Classe ExcecaoSintatica

```java
public class ExcecaoSintatica extends RuntimeException
```

Definida mas não usada ativamente no fluxo principal. Existe como mecanismo de emergência para erros irrecuperáveis.

---

## Classe InformacaoTipo

```java
class InformacaoTipo {
    private final String nome;       // ex: "int", "String", "MinhaClasse"
    private final int dimensoes;     // 0 = escalar, 1 = array, 2 = matriz
}
```

### comoTexto()

```java
String comoTexto()
```

Converte para texto com sufixos de array:
- `nome="int"`, `dimensoes=0` → `"int"`
- `nome="int"`, `dimensoes=1` → `"int[]"`
- `nome="String"`, `dimensoes=2` → `"String[][]"`

---

## Análise de Tipos

### analisarTipoOuVazio()

```java
private InformacaoTipo analisarTipoOuVazio()
```

- Se `void`, retorna `InformacaoTipo("void", 0)`
- Caso contrário, delega para `analisarTipo()`
- Usado para tipos de retorno de métodos

### analisarTipo()

```java
private InformacaoTipo analisarTipo()
```

- Se tipo primitivo (`int`, `double`, `boolean`, `char`, `float`, `long`) ou `String`: consome e usa como nome
- Caso contrário: chama `analisarNomeQualificado()` (para tipos como `java.util.List`)
- Depois: `analisarSufixoArrayTipo()` para contar dimensões `[]`

### ehInicioDeclaracao()

```java
private boolean ehInicioDeclaracao()
```

- Retorna `true` se o token atual parece iniciar uma declaração de variável
- Para tipos primitivos e `String`: sempre `true`
- Para identificadores: só `true` se `tokenSeguinte` também é identificador (heurística LL(1))

---

## Modificadores Reconhecidos

```java
private boolean ehModificador(String lexema) {
    return "public".equals(lexema) || "private".equals(lexema)
        || "protected".equals(lexema) || "static".equals(lexema)
        || "final".equals(lexema);
}
```

---

## Literais Reconhecidos

```java
private boolean ehLiteral() {
    return tokenAtual.codigo == TOKEN_NUMERO_INTEIRO
        || tokenAtual.codigo == TOKEN_NUMERO_REAL
        || tokenAtual.codigo == TOKEN_STRING
        || tokenAtual.codigo == TOKEN_CHAR_LITERAL
        || ehLexema("true") || ehLexema("false") || ehLexema("null");
}
```

---

## Expressão New

```
ExpressaoCriacao → NomeTipo '(' ListaArgumentos ')'
                 | NomeTipo '[' Expressao ']' ('[' Expressao? ']')*

analisarExpressaoCriacao()
    └─ analisarNomeTipoCriacao()
    └─ se '(' → analisarListaArgumentos() + consumir ')'
    └─ senão  → analisarRestanteCriacaoArray()
```

---

## Palavras Reservadas Reconhecidas pelo Parser

O parser tem o seu próprio conjunto de palavras reservadas (mais completo que o lexer):

```java
PALAVRAS_RESERVADAS = { "package", "import", "class", "extends",
    "public", "private", "protected", "static", "final",
    "void", "if", "else", "while", "for", "return", "break", "continue",
    "new", "this", "super", "true", "false", "null",
    "int", "double", "boolean", "char", "float", "long", "String" }
```

O método `ehIdentificador()` retorna `false` para qualquer token que esteja neste conjunto, mesmo que o lexer o tenha classificado como `TOKEN_IDENTIFICADOR`.

---

## Fluxo Completo da Fase 2

```
AnalisadorSintatico(lexer)
    └─ lerToken() × 2 → tokenAtual, tokenSeguinte
        │
        ▼
analisarPrograma()
    ├─ analisarDeclaracaoPacote()
    ├─ analisarDeclaracaoImportacao() × N
    └─ analisarDeclaracaoTipo() × N
        └─ analisarDeclaracaoClasse()
            ├─ entrarEscopo("NomeClasse", "classe")
            ├─ analisarMembroClasse() × N
            │   ├─ analisarRestanteMetodo()
            │   │   ├─ entrarEscopo("nomeMetodo", "metodo")
            │   │   ├─ analisarListaParametros()
            │   │   ├─ analisarCorpoMetodo()
            │   │   │   └─ analisarComando() × N
            │   │   └─ sairEscopo()
            │   └─ analisarRestanteCampo()
            └─ sairEscopo()
        │
        ▼
tabelaSimbolos.imprimir()  (se --debug ou --tabela)
```
