# Modo Panico

# Modo Pânico — Documentação Completa para Defesa Académica

Baseado na implementação real de `AnalisadorSintatico.java`

---

# PARTE 1 — VISÃO GERAL

## O que é o Modo Pânico

O modo pânico é uma estratégia de **recuperação de erros sintáticos**. Quando o parser encontra um token que não esperava, em vez de abortar imediatamente a compilação, ele:

1. Regista o erro
2. Descarta tokens do ficheiro fonte até encontrar um ponto onde a análise pode continuar com segurança
3. Retoma a análise normalmente a partir desse ponto

## Porque Existe

Sem o modo pânico, o parser pararia no primeiro erro. O programador teria que corrigir esse erro, recompilar, encontrar o segundo erro, corrigir, recompilar, etc. Com o modo pânico, o compilador consegue reportar **múltiplos erros numa única execução**.

## Qual Problema Resolve

O problema fundamental é: **depois de um erro, em que token o parser está?** O stream de tokens ficou numa posição inesperada. O parser não sabe mais onde está na gramática. Para recuperar, precisa de encontrar um ponto de referência conhecido — um token estruturalmente seguro para continuar.

## Como Foi Implementado Neste Projeto

A implementação usa a estratégia clássica de **modo pânico com conjuntos de sincronização**:

- Existem 4 conjuntos de tokens de sincronização, cada um adequado a um contexto gramatical diferente
- Quando ocorre um erro, o parser chama o método de sincronização adequado ao contexto
- A sincronização descarta tokens até encontrar um que pertença ao conjunto
- A análise retoma a partir desse token

Não usa exceções. Não aborta. Não tem `throw`. Tudo é feito por controlo de fluxo normal com `while`, `if` e `return`.

---

# PARTE 2 — LOCALIZAÇÃO NO CÓDIGO

## Classe Principal

`parser/AnalisadorSintatico.java`

## Atributos Diretamente Envolvidos no Modo Pânico

```java
private final List<ErroSintatico> listaErros = new ArrayList<ErroSintatico>();
// Acumula todos os erros encontrados durante a análise

private Token tokenAtual;
// Token que está a ser analisado agora — é o que dispara o erro

private Token tokenSeguinte;
// Próximo token — usado na recuperação local de consumirLexema()

private String ultimoErro = "";
// Chave do último erro registado — evita registar o mesmo erro duas vezes
```

## Conjuntos de Sincronização (Constantes Estáticas)

```java
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
```

## Todos os Métodos Envolvidos no Modo Pânico

| Método | Tipo | Responsabilidade |
|---|---|---|
| `relatarErro(String, String)` | private | Regista um `ErroSintatico`, evita duplicados |
| `sincronizar(Set<String>)` | private | Motor central — descarta tokens até sincronização |
| `sincronizarDeclaracao()` | private | Sinc. para contexto de declarações |
| `sincronizarComando()` | private | Sinc. para contexto de comandos/blocos |
| `sincronizarExpressao()` | private | Sinc. para contexto de expressões |
| `sincronizarEstrutura()` | private | Sinc. para estruturas de controlo |
| `consumirFimDeclaracao(String)` | private | Tenta consumir `;`, senão sinc. declaração |
| `consumirLexema(String, String)` | private | Recuperação local (sem sincronização completa) |
| `consumirIdentificador(String)` | private | Recuperação local para identificadores |
| `sintetico(String)` | private | Cria token fictício para continuar análise |
| `descrever(Token)` | private | Formata mensagem de erro legível |

## Mapa de Chamadas Real

```
analisarPrograma()
  ├─ [sem "package"] → relatarErro() → sincronizar({"import","class",...})
  └─ analisarDeclaracaoTipo()
       ├─ [sem "class"] → relatarErro() → sincronizarDeclaracao()
       └─ analisarDeclaracaoClasse()
            ├─ consumirLexema("class",...)  ─→ [erro] → relatarErro()
            ├─ consumirIdentificador(...)   ─→ [erro] → relatarErro() + avancar local
            ├─ consumirLexema("{",...)      ─→ [erro] → relatarErro()
            └─ analisarMembroClasse()
                 ├─ [sem tipo] → relatarErro() → sincronizarDeclaracao()
                 └─ analisarRestanteMetodo()
                      └─ analisarCorpoMetodo()
                           └─ analisarComando()
                                ├─ analisarComandoSe()
                                │    └─ [sem ")"] → relatarErro() → sincronizarEstrutura()
                                ├─ analisarComandoEnquanto()
                                │    └─ [sem ")"] → relatarErro() → sincronizarEstrutura()
                                ├─ analisarExpressao()
                                │    └─ analisarExpressaoPrimaria()
                                │         └─ [sem expr] → relatarErro() → sincronizarExpressao()
                                └─ [sem ";"] → relatarErro() → sincronizarComando()

consumirFimDeclaracao(contexto)
  ├─ [tem ";"] → verificarLexema(";") → ok
  └─ [sem ";"] → relatarErro("';'", contexto) → sincronizarDeclaracao()
```

---

# PARTE 3 — SEQUÊNCIA REAL DE EXECUÇÃO

## Código com Erro

```java
package teste;

class Programa {
    int x
    int y;
}
```

O erro: falta o `;` após `int x` (linha 4).

## Rastreio Passo a Passo

### PASSO 1 — Construtor, pré-carrega tokens

```
AnalisadorSintatico(lexer):
  tokenAtual    = lerToken() → Token("package", ...)
  tokenSeguinte = lerToken() → Token("teste", TOKEN_IDENTIFICADOR)
```

`analisarPrograma()` é invocado pelo `Main`.

### PASSO 2 — Pacote processado sem erros

```
analisarPrograma():
  ehLexema("package") == true → analisarDeclaracaoPacote()
    consumirLexema("package",...) → avança
    analisarNomeQualificado(...) → consome "teste"
    consumirFimDeclaracao(...) → consome ";"
```

tokenAtual avança para `"class"`.

### PASSO 3 — Classe processada sem erros

```
analisarDeclaracaoTipo() → analisarModificadores() → [] (vazio)
  ehLexema("class") == true → analisarDeclaracaoClasse([])
    consumirLexema("class",...) → avança
    consumirIdentificador(...) → consome "Programa"
    consumirLexema("{",...) → consome "{"
    tabelaSimbolos.entrarEscopo("Programa", "classe")
```

tokenAtual = `"int"` (primeiro membro da classe).

### PASSO 4 — Analisa `int x`

```
analisarMembroClasse():
  analisarModificadores() → []
  ehInicioTipo(true) → ehTipoPrimitivo("int") == true → ok
  analisarTipoOuVazio() → analisarTipo():
    nome = "int", avancar()
    retorna InformacaoTipo("int", 0)
  tokenAtual agora = "x"
  consumirIdentificador("membro da classe"):
    ehIdentificador() → true
    tokenConsumido = Token("x",...)
    avancar()
  tokenAtual agora = "int"  ← segundo "int" (linha 5)
```

### PASSO 5 — Decisão: campo ou método?

```
  verificarLexema("(") → ehLexema("(") → tokenAtual="int" → false
  → vai para analisarRestanteCampo([], InformacaoTipo("int",0), Token("x",...))
```

### PASSO 6 — ERRO DETECTADO

```
analisarRestanteCampo():
  declararVariavel(Token("x",...), ...) → símbolo "x" criado e declarado
  verificarLexema(",") → tokenAtual="int" → false → sai do loop
  consumirFimDeclaracao("declaracao de atributo"):
    verificarLexema(";"):
      ehLexema(";") → tokenAtual="int" → FALSE  ← ERRO AQUI
      retorna false
    → relatarErro("';'", "declaracao de atributo")   ← PASSO 7
    → sincronizarDeclaracao()                         ← PASSO 8
```

### PASSO 7 — relatarErro() regista o erro

```java
relatarErro("';'", "declaracao de atributo"):
  chave = "4|';'|int|declaracao de atributo"
  chave != ultimoErro ("") → regista:
  listaErros.add(ErroSintatico(4, "';'", "'int' (INT)", "declaracao de atributo"))
  ultimoErro = "4|';'|int|declaracao de atributo"
```

Mensagem produzida:
```
Erro Sintatico na linha 4 [declaracao de atributo]: esperado ';', mas encontrado 'int' (INT)
```

### PASSO 8 — MODO PÂNICO INICIA: sincronizarDeclaracao()

```java
private void sincronizarDeclaracao() {
    sincronizar(SINCRONIZACAO_DECLARACAO);  // motor do pânico
    if (ehLexema(";")) {
        avancar();
    }
}
```

```java
private void sincronizar(Set<String> sincronizacao) {
    while (!fim() && !sincronizacao.contains(tokenAtual.lexema)) {
        avancar();
    }
}
```

Estado ao entrar em `sincronizar()`:
```
tokenAtual.lexema = "int"
SINCRONIZACAO_DECLARACAO contém "int" → TRUE
loop não executa nenhuma iteração
```

Retorna a `sincronizarDeclaracao()`:
```java
ehLexema(";") → "int" → false → não avança extra
```

### PASSO 9 — SAÍDA DO MODO PÂNICO

O controlo retorna a `consumirFimDeclaracao()` → `analisarRestanteCampo()` → `analisarMembroClasse()` → loop em `analisarDeclaracaoClasse()`:

```java
while (!fim() && !ehLexema("}")) {
    analisarMembroClasse();   // nova iteração com tokenAtual="int"
}
```

### PASSO 10 — Segundo membro `int y;` analisado normalmente

```
analisarMembroClasse():
  analisarTipo() → "int" → avança
  consumirIdentificador() → "y" → avança
  verificarLexema("(") → false → campo
  analisarRestanteCampo():
    declararVariavel(Token("y",...), ...)
    consumirFimDeclaracao():
      verificarLexema(";") → tokenAtual=";" → TRUE → avança → ok
```

Sem erros.

### PASSO 11 — Fecha classe, fim da análise

```
tokenAtual = "}"
loop: !ehLexema("}") → false → sai
consumirLexema("}", "corpo da classe") → consome "}"
tabelaSimbolos.sairEscopo()
```

### Resultado Final

```
Total de erros: 1
[1] Erro Sintatico na linha 4 [declaracao de atributo]: esperado ';', mas encontrado 'int' (INT)
```

A variável `y` foi correctamente declarada na tabela de símbolos apesar do erro anterior.

---

# PARTE 4 — SINCRONIZAÇÃO

## O Motor Central: sincronizar()

```java
private void sincronizar(Set<String> sincronizacao) {
    while (!fim() && !sincronizacao.contains(tokenAtual.lexema)) {
        avancar();
    }
}
```

Duas condições de paragem:
1. `fim()` — chegou ao TOKEN_FIM_ARQUIVO
2. `sincronizacao.contains(tokenAtual.lexema)` — token de sincronização encontrado

## SINCRONIZACAO_DECLARACAO — usado por sincronizarDeclaracao()

Após sincronizar, consome `;` se presente.

| Token | Categoria | Motivo |
|---|---|---|
| `;` | delimitador | fim de declaração |
| `}` | delimitador | fim de bloco/classe |
| `class` | palavra reservada | início de nova classe |
| `public` `private` `protected` | modificadores | início de novo membro |
| `static` `final` | modificadores | início de novo membro |
| `void` | tipo de retorno | início de método |
| `int` `double` `boolean` `char` `float` `long` `String` | tipos | início de declaração |

## SINCRONIZACAO_BLOCO — usado por sincronizarComando()

Após sincronizar, consome `;` se presente.

| Token | Categoria | Motivo |
|---|---|---|
| `}` | delimitador | fim do bloco atual |
| `{` | delimitador | início de sub-bloco |
| `if` `while` `for` `return` `break` `continue` | comandos | início de novo comando |
| `int` `double` `boolean` `char` `float` `long` `String` | tipos | início de declaração |

## SINCRONIZACAO_EXPRESSAO — usado por sincronizarExpressao()

Não consome token extra após sincronizar.

| Token | Categoria | Motivo |
|---|---|---|
| `;` | delimitador | fim do comando |
| `)` | delimitador | fim de condição/chamada |
| `]` | delimitador | fim de índice de array |
| `,` | separador | próximo argumento/variável |
| `}` | delimitador | fim de bloco |
| `:` | operador | parte do ternário `?:` |
| `{` | delimitador | início de bloco |

## SINCRONIZACAO_ESTRUTURA — usado por sincronizarEstrutura()

Não consome token extra após sincronizar.

| Token | Categoria | Motivo |
|---|---|---|
| `)` | delimitador | fecha condição do if/while |
| `{` | delimitador | início do corpo |
| `}` | delimitador | fim de bloco pai |
| `;` | delimitador | fim de declaração |
| `else` | palavra reservada | ramo else do if |

## Tabela de Decisão por Token

```
Token Encontrado              | Presente em         | Ação em sincronizar()
------------------------------|---------------------|----------------------
";"                           | DECL, EXPR          | PARA
"}"                           | DECL, BLOCO, EXPR, ESTRUT | PARA
"{"                           | BLOCO, EXPR, ESTRUT | PARA
"int","double","float"...     | DECL, BLOCO         | PARA
"if","while","for"...         | BLOCO               | PARA
"return","break","continue"   | BLOCO               | PARA
")"                           | EXPR, ESTRUT        | PARA
"]"                           | EXPR                | PARA
","                           | EXPR                | PARA
":"                           | EXPR                | PARA
"class"                       | DECL                | PARA
"public","private"...         | DECL                | PARA
"else"                        | ESTRUT              | PARA
identificador qualquer        | nenhum              | DESCARTA (avancar)
número, string, char literal  | nenhum              | DESCARTA (avancar)
operadores +,-,*,/,...        | nenhum              | DESCARTA (avancar)
TOKEN_FIM_ARQUIVO             | (fim())             | PARA sempre
```

---

# PARTE 5 — SAÍDA DO MODO PÂNICO

## Condição de Saída

```java
while (!fim() && !sincronizacao.contains(tokenAtual.lexema)) {
    avancar();
}
// saída: fim()==true  OU  tokenAtual pertence ao conjunto
```

## Retorno ao Fluxo Normal

```
sincronizar() termina
       |
sincronizarDeclaracao()/sincronizarComando()/etc. retornam
       |
consumirFimDeclaracao() retorna  (se foi o ponto de entrada)
       |
método de análise (analisarRestanteCampo, etc.) retorna
       |
analisarMembroClasse() ou analisarComando() retorna
       |
loop: while (!fim() && !ehLexema("}")) { analisarMembroClasse(); }
       |
nova iteração — tokenAtual é o token de sincronização
análise continua normalmente
```

## Como Evita Loops Infinitos

### Mecanismo 1 — fim() como guarda universal

```java
while (!fim() && ...)
```

Se o ficheiro acabar, `fim()` retorna `true` e o loop para. Garante terminação absoluta.

### Mecanismo 2 — avancar() explícito em consumirIdentificador()

```java
private Token consumirIdentificador(String contexto) {
    if (ehIdentificador()) { ... retorna token ... }
    relatarErro("identificador", contexto);
    Token tokenSintetico = sintetico("<missing>");
    if (!fim() && !ehLexema(";") && !ehLexema(")") && !ehLexema("}") && !ehLexema(",")) {
        avancar();  // garante progressão quando consumirIdentificador falha
    }
    return tokenSintetico;
}
```

Este `avancar()` garante que o parser nunca fica preso no mesmo token quando `consumirIdentificador()` falha repetidamente.

### Mecanismo 3 — Tokens de sincronização cobrem toda a estrutura Java

Os conjuntos incluem tipos primitivos, modificadores e estruturas de controlo — tokens que aparecem em qualquer programa Java, por isso a sincronização raramente percorre muitos tokens.

## Como Evita Erros em Cascata — ultimoErro

```java
private void relatarErro(String esperado, String contexto) {
    String chave = tokenAtual.linha + "|" + esperado + "|" + tokenAtual.lexema + "|" + contexto;
    if (!chave.equals(ultimoErro)) {
        listaErros.add(new ErroSintatico(tokenAtual.linha, esperado, descrever(tokenAtual), contexto));
        ultimoErro = chave;
    }
}
```

A chave combina `linha + esperado + tokenAtual.lexema + contexto`. Se duas chamadas consecutivas tentarem registar o mesmo erro sobre o mesmo token, a segunda é silenciada. Depois de `sincronizar()` mover o `tokenAtual`, a chave muda e novos erros genuínos são registados normalmente.

---

# PARTE 6 — EXEMPLO COMPLETO COM MÚLTIPLOS ERROS

## Código Fonte

```java
package demo;

public class Teste {
    public int calcular(int a int b) {
        int resultado = a + ;
        if (a > b {
            return resultado
        }
        return 0;
    }
}
```

Erros presentes:
- Linha 4: falta `,` entre parâmetros (`int a int b`)
- Linha 5: expressão incompleta (`a + ;`)
- Linha 6: falta `)` na condição do if (`if (a > b {`)
- Linha 7: falta `;` após return (`return resultado`)

---

## Erro 1 — Falta vírgula entre parâmetros (linha 4)

```
Contexto: analisarListaParametros(metodo)

  analisarParametro() → consome "int" e "a" → ok
  verificarLexema(",") → tokenAtual="int" → false → sai do while
  consumirLexema(")", "lista de parametros"):
    ehLexema(")") → tokenAtual="int" → FALSE
    relatarErro("')'", "lista de parametros")
       → ErroSintatico(4, "')'", "'int' (INT)", "lista de parametros")
    ehLexemaSeguinte(")") → tokenSeguinte="b" → FALSE
    return sintetico(")")   ← token fictício, stream não avança
```

O parser continua com `)` sintético e tenta processar `"int" "b"` como parte do corpo.

---

## Erro 2 — Expressão incompleta: `a + ;` (linha 5)

```
Contexto: analisarExpressaoPrimaria

  "a" → identificador → ok → avança
  "+" → operador aditivo → avança
  tokenAtual = ";"

  analisarExpressaoPrimaria():
    ehLiteral() → false
    ehIdentificador() → false
    verificarLexema("this"/"super"/"("/"new") → false
    relatarErro("expressao", "expressao primaria")
       → ErroSintatico(5, "expressao", "';' (PONTO_VIRGULA)", "expressao primaria")
    sincronizarExpressao()
       sincronizar(SINCRONIZACAO_EXPRESSAO)
       tokenAtual=";" → pertence → para IMEDIATAMENTE (0 tokens descartados)
    retorna

analisarComando():
  if (!verificarLexema(";")) → ";" → TRUE → avança → ok
```

---

## Erro 3 — Falta `)` no if (linha 6)

```
Contexto: analisarComandoSe

  consumirLexema("if",...) → ok
  consumirLexema("(",...) → ok
  analisarExpressao() → "a > b" → ok
  tokenAtual = "{"    ← esperava ")"

  if (!verificarLexema(")")) {
    relatarErro("')'", "condicao if")
       → ErroSintatico(6, "')'", "'{' (ABRE_CHAVE)", "condicao if")
    sincronizarEstrutura()
       sincronizar(SINCRONIZACAO_ESTRUTURA = {")", "{", "}", ";", "else"})
       tokenAtual="{" → pertence → para IMEDIATAMENTE
    verificarLexema(")") → "{" → false → não avança
  }

  analisarComando():
    ehLexema("{") → TRUE → analisarBloco() → corpo do if analisado normalmente
```

---

## Erro 4 — Falta `;` após return (linha 7)

```
Contexto: analisarComandoRetorno

  consumirLexema("return",...) → ok
  !ehLexema(";") → "resultado" → TRUE → analisarExpressao()
    → "resultado" → tabelaSimbolos.resolver("resultado") → encontra → ok → avança
  tokenAtual = "}"    ← esperava ";"

  consumirFimDeclaracao("comando return"):
    verificarLexema(";") → "}" → FALSE
    relatarErro("';'", "comando return")
       → ErroSintatico(7, "';'", "'}' (FECHA_CHAVE)", "comando return")
    sincronizarDeclaracao()
       sincronizar(SINCRONIZACAO_DECLARACAO)
       tokenAtual="}" → pertence → para IMEDIATAMENTE
       ehLexema(";") → "}" → false → não avança extra

loop analisarBloco(): !ehLexema("}") → false → sai
consumirLexema("}") → ok → análise termina normalmente
```

---

## Resultado Final

```
Total de erros: 4

[1] Erro Sintatico na linha 4 [lista de parametros]: esperado ')', mas encontrado 'int' (INT)
[2] Erro Sintatico na linha 5 [expressao primaria]: esperado expressao, mas encontrado ';' (PONTO_VIRGULA)
[3] Erro Sintatico na linha 6 [condicao if]: esperado ')', mas encontrado '{' (ABRE_CHAVE)
[4] Erro Sintatico na linha 7 [comando return]: esperado ';', mas encontrado '}' (FECHA_CHAVE)
```

---

# PARTE 7 — DIAGRAMAS ASCII

## Diagrama 1 — Fluxo Geral do Modo Pânico

```
     Token inesperado encontrado
                  |
                  v
    consumirLexema() / consumirFimDeclaracao()
    / analisarExpressaoPrimaria() / analisarMembroClasse()
                  |
                  v
         relatarErro(esperado, contexto)
                  |
         .--------+--------.
         |                 |
   chave == ultimoErro   chave != ultimoErro
         |                 |
      (ignora)       listaErros.add(ErroSintatico)
                     ultimoErro = chave
                             |
                             v
                  sincronizar*() chamado
                  (adequado ao contexto)
                             |
                             v
          .-------------------------------------------.
          |  while (!fim() &&                         |
          |    !conjunto.contains(tokenAtual.lexema)) |
          |        avancar()  <- descarta token       |
          '-------------------------------------------'
                             |
              .──────────────+──────────────.
              |                             |
         fim()==true              token no conjunto
              |                             |
        EOF atingido          token de sincronizacao
        para tudo             encontrado -- para o loop
                                            |
                                            v
                              retorna ao metodo chamador
                                            |
                                            v
                              sobe na pilha de chamadas
                                            |
                                            v
                   loop principal itera com token de sincronizacao
                                            |
                                            v
                              analise continua normalmente
```

## Diagrama 2 — Hierarquia de Sincronização por Contexto

```
analisarPrograma()
  |
  +-[sem "package"]---> sincronizar({"import","class","public",...})
  |
  '--> analisarDeclaracaoTipo()
         |
         +-[sem "class"]---> sincronizarDeclaracao()
         |                     '--> sincronizar(SINCRONIZACAO_DECLARACAO)
         |                     '--> consome ";" se presente
         |
         '--> analisarDeclaracaoClasse()
                '--> analisarMembroClasse()
                       |
                       +-[sem tipo]---> sincronizarDeclaracao()
                       |
                       '--> analisarCorpoMetodo()
                              '--> analisarComando()
                                     |
                                     +-[sem ";"]---> sincronizarComando()
                                     |                '--> sincronizar(SINCRONIZACAO_BLOCO)
                                     |                '--> consome ";" se presente
                                     |
                                     +--> analisarComandoSe()
                                     |      '-[sem ")"]---> sincronizarEstrutura()
                                     |                        '--> sincronizar(SINCRONIZACAO_ESTRUTURA)
                                     |
                                     +--> analisarComandoEnquanto()
                                     |      '-[sem ")"]---> sincronizarEstrutura()
                                     |
                                     '--> analisarExpressao()
                                            '--> analisarExpressaoPrimaria()
                                                   '-[sem expr]---> sincronizarExpressao()
                                                                      '--> sincronizar(SINCRONIZACAO_EXPRESSAO)
```

## Diagrama 3 — sincronizar() internamente

```
entrada: tokenAtual = X

.--------------------------------------------.
|                                            |
|  .-- fim() ? --YES-------------------> PARA|
|  |                                         |
|  NO                                        |
|  |                                         |
|  '--> conjunto.contains(tokenAtual.lexema)?|
|              |                             |
|            YES ----------------------> PARA|
|              |                  (token     |
|             NO                   seguro)   |
|              |                             |
|          avancar()                         |
|          tokenAtual   <- tokenSeguinte     |
|          tokenSeguinte <- lerToken(lexer)  |
|              |                             |
|          volta ao topo do loop             |
'--------------------------------------------'
```

## Diagrama 4 — consumirLexema() com recuperação local

```
consumirLexema(esperado, contexto)
          |
    ehLexema(esperado)?
          |
    .-----+------.
   YES            NO
    |              |
 avancar()     relatarErro(esperado, contexto)
 retorna token         |
                ehLexemaSeguinte(esperado)?
                       |
                 .-----+------.
                YES            NO
                 |              |
             avancar()     return sintetico(esperado)
             (salta erro)  (ficticio, nao avanca)
             avancar()
             (consome)
             retorna token
```

## Diagrama 5 — consumirFimDeclaracao()

```
consumirFimDeclaracao(contexto)
          |
    verificarLexema(";")
    [ehLexema(";") && avancar()]
          |
    .-----+------.
   YES            NO
    |              |
  retorna      relatarErro("';'", contexto)
  (sem erro)         |
                sincronizarDeclaracao()
                     |
                sincronizar(SINCRONIZACAO_DECLARACAO)
                     |    [descarta ate token seguro]
                     |
                ehLexema(";") ?
                     |
               .-----+------.
              YES             NO
               |               |
           avancar()         retorna
           retorna           (token seguro fica
                              em tokenAtual)
```

---

# PARTE 8 — PERGUNTAS DE DEFESA

## Pergunta 1

**O que é o modo pânico?**

Resposta curta: é uma estratégia de recuperação de erros que, após detetar um token inesperado, descarta tokens até encontrar um ponto estruturalmente seguro para continuar a análise.

Resposta detalhada: quando o parser encontra um token inesperado, chama `relatarErro()` para registar o erro e depois chama um dos métodos `sincronizar*()`. Esse método executa um `while (!fim() && !conjunto.contains(tokenAtual.lexema)) { avancar(); }` até que `tokenAtual` pertença ao conjunto de sincronização. Após isso, o controlo retorna ao loop principal do parser (`analisarMembroClasse()` ou `analisarComando()`) e a análise continua.

---

## Pergunta 2

**Porque escolheu esta estratégia e não outra?**

Resposta curta: o modo pânico é simples de implementar, robusto, e diretamente compatível com parsers descendentes recursivos como este.

Resposta detalhada: existem outras estratégias — recuperação por frase, produções de erro, correção global. O modo pânico foi escolhido por não requerer modificações à gramática, por ser previsível (o programador sabe exatamente quais tokens servem de pontos de recuperação em cada contexto), e por se encaixar naturalmente na estrutura de métodos recursivos onde cada método conhece o seu contexto e o seu conjunto de sincronização adequado.

---

## Pergunta 3

**Como o parser sai do modo pânico?**

Resposta curta: quando `tokenAtual.lexema` pertence ao conjunto de sincronização do contexto, ou quando o ficheiro termina.

Resposta detalhada: o método `sincronizar(Set<String> sincronizacao)` contém `while (!fim() && !sincronizacao.contains(tokenAtual.lexema)) { avancar(); }`. O loop termina quando `fim()` retorna `true` (TOKEN_FIM_ARQUIVO) ou quando `tokenAtual.lexema` está no conjunto. Após o loop, `sincronizar()` retorna, e o método que o chamou retorna também, subindo na pilha de chamadas até o loop principal do parser, que itera com o token de sincronização como ponto de partida.

---

## Pergunta 4

**O que acontece se nunca encontrar um token de sincronização?**

Resposta curta: o parser chega ao fim do ficheiro e para graciosamente, sem lançar exceções.

Resposta detalhada: a condição `!fim()` no loop garante que, se nenhum token do conjunto for encontrado até ao fim do ficheiro, o loop termina quando `tokenAtual.codigo == TOKEN_FIM_ARQUIVO`. O método retorna, o parser encerra o fluxo normal, e o `Main` reporta todos os erros registados em `listaErros`. Nenhuma exceção é lançada. É uma terminação graciosa e controlada.

---

## Pergunta 5

**Como evita loops infinitos?**

Resposta curta: três mecanismos: `fim()` como guarda universal, `avancar()` explícito em `consumirIdentificador()`, e tokens de sincronização bem escolhidos.

Resposta detalhada: primeiro, `fim()` garante que o loop sempre termina no EOF. Segundo, `consumirIdentificador()` chama `avancar()` explicitamente se não encontrar identificador e o token atual não é um delimitador — isto garante que o parser não fica preso no mesmo token quando `consumirIdentificador()` falha. Terceiro, os tokens de sincronização incluem tipos primitivos, delimitadores e comandos — tokens que aparecem com muita frequência em código Java, limitando o número de tokens descartados.

---

## Pergunta 6

**Porque não interromper imediatamente a compilação no primeiro erro?**

Resposta curta: porque o programador beneficia de ver todos os erros de uma vez, em vez de um ciclo lento de corrigir-recompilar.

Resposta detalhada: se o compilador abortasse no primeiro erro, o programador teria um ciclo lento e frustrante. Com o modo pânico, uma única execução pode reportar vários erros independentes — por exemplo, 4 erros em 4 linhas diferentes — permitindo corrigir todos de uma vez. Esta é uma funcionalidade de usabilidade fundamental em qualquer compilador de uso real.

---

## Pergunta 7

**Qual é a diferença entre os quatro métodos de sincronização?**

Resposta curta: cada um usa um conjunto diferente adequado ao seu contexto, e dois deles consomem `;` depois de sincronizar.

Resposta detalhada:
- `sincronizarDeclaracao()` usa `SINCRONIZACAO_DECLARACAO` e depois consome `;` se presente. Adequado para erros em declarações de variáveis, campos e métodos.
- `sincronizarComando()` usa `SINCRONIZACAO_BLOCO` e depois consome `;` se presente. Adequado para erros dentro de corpos de métodos e blocos.
- `sincronizarExpressao()` usa `SINCRONIZACAO_EXPRESSAO` e não consome token extra. O `;` é preservado para o contexto chamador que precisa de o consumir.
- `sincronizarEstrutura()` usa `SINCRONIZACAO_ESTRUTURA`. Adequado para erros nas condições de `if` e `while`, onde `)` e `{` precisam de ser preservados.

---

## Pergunta 8

**O que é o `ultimoErro` e porque existe?**

Resposta curta: é uma string que guarda a chave do último erro registado, usada para evitar registar o mesmo erro duas vezes consecutivas.

Resposta detalhada: a chave é `tokenAtual.linha + "|" + esperado + "|" + tokenAtual.lexema + "|" + contexto`. Se duas chamadas consecutivas a `relatarErro()` produzirem a mesma chave, a segunda é silenciada. Isto é necessário porque em parsers recursivos, o mesmo token pode ser processado por múltiplas chamadas na pilha antes de qualquer sincronização ocorrer — sem este mecanismo, o mesmo erro apareceria várias vezes na lista de erros, confundindo o programador.

---

## Pergunta 9

**O que é um token sintético e quando é criado?**

Resposta curta: é um `Token` fictício com o lexema esperado, criado quando o token real não está presente, para que o parser possa continuar sem avançar o stream.

Resposta detalhada: `sintetico(String lexema)` cria `new Token(lexema, TOKEN_IDENTIFICADOR, tokenAtual.linha)`. É chamado por `consumirLexema()` quando o token esperado não está em `tokenAtual` nem em `tokenSeguinte`. O parser recebe este token fictício e continua a análise — por exemplo, para criar um `Simbolo` com um nome de placeholder. O stream real de tokens não avança, e o erro já foi registado por `relatarErro()`.

---

## Pergunta 10

**Qual é a diferença entre consumirLexema() e sincronizar()?**

Resposta curta: `consumirLexema()` faz recuperação micro-local (salta no máximo um token); `sincronizar()` é a recuperação macro e pode descartar muitos tokens.

Resposta detalhada: `consumirLexema()` tenta uma recuperação de baixo custo — se o token esperado está no `tokenSeguinte`, salta apenas o token errado e continua. Se não está em nenhum dos dois, cria um token sintético sem descartar nada. Este é o mecanismo preferido para erros pontuais simples. `sincronizar()` é ativado quando `consumirLexema()` não resolve — descarta todos os tokens até encontrar um ponto estruturalmente seguro, podendo saltar partes inteiras do código fonte.

---

## Pergunta 11

**Porque é que `SINCRONIZACAO_BLOCO` inclui `{` mas `SINCRONIZACAO_DECLARACAO` não?**

Resposta curta: dentro de um bloco, `{` significa início de sub-bloco (ponto seguro); no contexto de campos de classe, `{` não é esperado.

Resposta detalhada: `sincronizarComando()` é usado dentro de corpos de métodos. Ali, encontrar `{` significa que está prestes a começar um sub-bloco — é um ponto de retoma válido para `analisarBloco()`. `sincronizarDeclaracao()` é usado no contexto de campos de classe, onde um `{` isolado não faz parte da gramática de declaração de campo e não seria um ponto estruturalmente seguro.

---

## Pergunta 12

**O modo pânico afeta a tabela de símbolos?**

Resposta curta: não diretamente — os símbolos já declarados antes do erro ficam na tabela; os tokens descartados pelo modo pânico não chegam a ser declarados.

Resposta detalhada: quando `sincronizar()` descarta tokens com `avancar()`, esses tokens não passam por nenhum método de declaração (`declararVariavel`, `declarar`, etc.). Qualquer declaração em curso no momento do erro é simplesmente abandonada. As declarações processadas com sucesso antes do erro permanecem na tabela. No exemplo da Parte 3, a variável `x` foi declarada antes do modo pânico e permanece na tabela; `y` foi declarada depois e também permanece correctamente.

---

## Pergunta 13

**Como funciona a recuperação dentro de `analisarComandoSe()`?**

Resposta curta: se o `)` não for encontrado após a condição, chama `sincronizarEstrutura()` e tenta consumir `)` mais uma vez.

Resposta detalhada:
```java
if (!verificarLexema(")")) {
    relatarErro("')'", "condicao if");
    sincronizarEstrutura();
    verificarLexema(")");
}
analisarComando();  // continua com o corpo do if
```
Após `sincronizarEstrutura()`, `tokenAtual` estará em `{")", "{", "}", ";", "else"}`. Se for `)`, `verificarLexema(")")` consome-o e a análise continua normalmente. Se for `{`, o `)` é assumido ausente e o parser tenta analisar o bloco directamente com `analisarComando()`.

---

## Pergunta 14

**Porque `sincronizarExpressao()` não consome `;` depois de sincronizar?**

Resposta curta: porque o `;` pertence ao comando que contém a expressão — se a expressão o consumisse, o comando pai produziria outro erro.

Resposta detalhada: `sincronizarExpressao()` é chamado de dentro de `analisarExpressaoPrimaria()`, chamada por `analisarComando()`. Após `sincronizarExpressao()`, o `tokenAtual` fica no token de sincronização (por exemplo `;`). O controlo regressa a `analisarComando()`, que tenta `if (!verificarLexema(";"))`. Se `sincronizarExpressao()` já tivesse consumido o `;`, `verificarLexema(";")` falharia e produziria um erro adicional falso.

---

## Pergunta 15

**Quantas vezes pode o modo pânico ser ativado numa única análise?**

Resposta curta: tantas vezes quantos os erros que requerem sincronização — não há limite.

Resposta detalhada: a lista `listaErros` pode crescer indefinidamente. Não há threshold que force paragem. Cada chamada a `sincronizarDeclaracao()`, `sincronizarComando()`, `sincronizarExpressao()` ou `sincronizarEstrutura()` é um ciclo de modo pânico independente. O compilador processa o ficheiro completo mesmo com centenas de erros, reportando todos no final.

---

## Pergunta 16

**Como o parser distingue um erro léxico de um erro sintático?**

Resposta curta: em `lerToken()`, quando o lexer devolve `TOKEN_ERRO`, é registado imediatamente como `ErroSintatico` com contexto `"analise lexica"`.

Resposta detalhada:
```java
private Token lerToken() {
    Token tokenLido;
    do {
        tokenLido = lexer.analex();
        tokenLido.linha = lexer.getLinhaAtual();
    } while (tokenLido.codigo == AnalisadorLexico.TOKEN_COMENTARIO);
    if (tokenLido.codigo == AnalisadorLexico.TOKEN_ERRO) {
        listaErros.add(new ErroSintatico(tokenLido.linha, "token valido",
            "'" + tokenLido.lexema + "' (ERRO)", "analise lexica"));
    }
    return tokenLido;
}
```
O token de erro entra na mesma `listaErros` que os erros sintáticos, mas com contexto `"analise lexica"` para os distinguir. O token é devolvido ao parser, que tentará processá-lo e provavelmente gerará também um erro sintático.

---

## Pergunta 17

**O que acontece quando `consumirLexema()` cria um token sintético — o parser avança?**

Resposta curta: não. O token sintético é devolvido mas `tokenAtual` não muda.

Resposta detalhada: `sintetico(lexema)` cria `new Token(lexema, TOKEN_IDENTIFICADOR, tokenAtual.linha)` e retorna-o sem chamar `avancar()`. O `tokenAtual` continua a apontar para o token que estava lá quando o erro ocorreu. O método que chamou `consumirLexema()` recebe o token fictício e usa-o (por exemplo, para criar um `Simbolo`), mas o próximo método que verificar `tokenAtual` verá ainda o token real que falhou.

---

## Pergunta 18

**Como é que `ehInicioDeclaracao()` evita ambiguidade entre declaração e expressão?**

Resposta curta: usa `tokenSeguinte` como lookahead — só considera declaração de tipo-nome se o segundo token também for identificador.

Resposta detalhada:
```java
private boolean ehInicioDeclaracao() {
    if (ehTipoPrimitivo(tokenAtual.lexema) || ehLexema("String")) {
        return true;  // int, double, etc. sao sempre declaracoes
    }
    return ehIdentificador() && tokenSeguinte.codigo == AnalisadorLexico.TOKEN_IDENTIFICADOR;
}
```
Para tipos primitivos, é sempre declaração. Para identificadores (ex: `MinhaClasse`), só é declaração se `tokenSeguinte` também for identificador — isto distingue `MinhaClasse x;` (declaração) de `minhaInstancia.metodo()` (expressão, onde `tokenSeguinte` seria `.`).

---

## Pergunta 19

**Qual é o comportamento quando o ficheiro começa sem `package`?**

Resposta curta: regista erro e sincroniza para `{"import","class","public","private","protected"}`, depois continua normalmente.

Resposta detalhada:
```java
if (ehLexema("package")) {
    analisarDeclaracaoPacote();
} else {
    relatarErro("'package'", "inicio do programa");
    sincronizar(new HashSet<String>(Arrays.asList(
        "import", "class", "public", "private", "protected")));
}
```
Este é o único uso de `sincronizar()` com um conjunto criado inline — não uma das quatro constantes estáticas. O conjunto inclui exactamente os tokens que podem iniciar conteúdo válido num ficheiro Java sem `package`. Após sincronização, o parser processa importações e declarações de tipo normalmente.

---

## Pergunta 20

**Qual a limitação mais importante do modo pânico implementado?**

Resposta curta: tokens válidos que ocorrem durante a janela de sincronização são descartados, podendo gerar erros falsos em cascata.

Resposta detalhada: quando `sincronizar()` descarta tokens, alguns podem fazer parte de declarações subsequentes válidas. Num ficheiro com muitos erros próximos, o modo pânico pode descartar código que seria correcto, e o parser ao retomar pode encontrar um contexto inesperado — gerando erros que não existem realmente no código original (erros em cascata). Por isso, ao interpretar erros reportados, deve-se começar sempre pelo primeiro e recompilar após cada correção, pois os erros posteriores podem ser consequência do anterior e não erros reais.
