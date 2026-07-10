# Fase 1 — Analisador Léxico

## Objetivo da Fase

O Analisador Léxico é a primeira fase do compilador. A sua responsabilidade é ler o código-fonte caractere a caractere e agrupar esses caracteres em unidades com significado chamadas **tokens**. Cada token tem um código numérico e um lexema (o texto original reconhecido).

O lexer não verifica se a estrutura do programa está correta — isso é trabalho do parser. O lexer apenas responde à pergunta: "que tipo de símbolo é este?"

---

## Classes Envolvidas

| Classe | Pacote | Ficheiro |
|---|---|---|
| `AnalisadorLexico` | `lexer` | `AnalisadorLexico.java` |
| `Token` | `lexer` | `Token.java` |
| `TabelaSimbolos` (léxica) | `lexer` | `TabelaSimbolos.java` |
| `TipoToken` | `utils` | `TipoToken.java` |

---

## Estrutura Interna do AnalisadorLexico

### Atributos principais

```
private char[] conteudo   → array com todos os caracteres do ficheiro fonte
private int pos           → posição atual de leitura no array
private int linha         → número da linha atual (começa em 1)
private int coluna        → número da coluna atual (começa em 1)
private TabelaSimbolos tabela  → tabela de símbolos léxica
private StringBuilder lexema  → buffer para construir o lexema atual
```

### Constantes de tokens (public static final int)

Cada tipo de token tem um código inteiro único. Exemplos:

```
TOKEN_IDENTIFICADOR    = 1
TOKEN_IF               = 2
TOKEN_WHILE            = 3
TOKEN_INT              = 4
TOKEN_FLOAT            = 5
TOKEN_RETURN           = 6
TOKEN_CLASS            = 7
TOKEN_PUBLIC           = 8
TOKEN_VOID             = 9
TOKEN_NUMERO_INTEIRO   = 10
TOKEN_NUMERO_REAL      = 11
TOKEN_OP_ADICAO        = 12
TOKEN_OP_SUBTRACAO     = 13
TOKEN_OP_MULTIPLICACAO = 14
TOKEN_OP_DIVISAO       = 15
TOKEN_OP_MODULO        = 16
TOKEN_OP_MENOR         = 17
TOKEN_OP_MAIOR         = 18
TOKEN_OP_MENOR_IGUAL   = 19
TOKEN_OP_MAIOR_IGUAL   = 20
TOKEN_OP_IGUAL         = 21
TOKEN_OP_DIFERENTE     = 22
TOKEN_OP_ATRIBUICAO    = 23
TOKEN_OP_AND           = 24
TOKEN_OP_OR            = 25
TOKEN_ABRE_PARENTESE   = 26
TOKEN_FECHA_PARENTESE  = 27
TOKEN_ABRE_CHAVE       = 28
TOKEN_FECHA_CHAVE      = 29
TOKEN_ABRE_COLCHETE    = 30
TOKEN_FECHA_COLCHETE   = 31
TOKEN_PONTO_VIRGULA    = 32
TOKEN_VIRGULA          = 33
TOKEN_PONTO            = 34
TOKEN_STRING           = 35
TOKEN_COMENTARIO       = 36
TOKEN_FIM_ARQUIVO      = 37
TOKEN_ERRO             = 38
TOKEN_OP_INCREMENTO    = 39
TOKEN_OP_DECREMENTO    = 40
TOKEN_OP_NOT           = 41
TOKEN_CHAR_LITERAL     = 42
TOKEN_DOIS_PONTOS      = 43
TOKEN_OP_BIT_AND       = 44
TOKEN_OP_BIT_OR        = 45
TOKEN_OP_BIT_XOR       = 46
TOKEN_INTERROGACAO     = 47
```

---

## Construtor

```java
public AnalisadorLexico(String caminho)
```

- Lê o ficheiro fonte completo com `Files.readAllBytes`
- Converte para `char[]` e armazena em `conteudo`
- Inicializa `TabelaSimbolos` e `StringBuilder lexema`
- Em caso de erro de I/O, `conteudo` fica vazio

---

## Método iniciarAnalise()

```java
public void iniciarAnalise()
```

- Chamado quando o lexer é usado de forma autónoma (sem parser)
- Chama `analex()` em loop até receber `TOKEN_FIM_ARQUIVO`
- Para cada token, chama `gravarTokenLexema()` para registar na tabela
- No final, chama `tabela.mostrar()` para imprimir todos os tokens

Fluxo:
```
iniciarAnalise()
    └─ loop: analex() → token
        └─ gravarTokenLexema(token.lexema, token.codigo)
    └─ tabela.mostrar()
```

---

## Método analex() — O Coração do Lexer

```java
public Token analex()
```

- Chamado pelo parser a cada vez que precisa do próximo token
- Implementa um **Autómato Finito Determinístico (DFA)** com mais de 60 estados
- Usa um `switch(estado)` dentro de um `while(true)`
- Começa sempre no estado 0
- Retorna um `Token` quando reconhece um lexema completo

### Máquina de Estados — Visão Geral

```
Estado 0 (Inicial)
│
├─ whitespace (' ', '\t', '\n', '\r') → ignorar, continuar no estado 0
├─ '\0' → retornar TOKEN_FIM_ARQUIVO
│
├─ letra minúscula
│   ├─ 'i' → estado 10  (if / int)
│   ├─ 'w' → estado 20  (while)
│   ├─ 'f' → estado 40  (float)
│   ├─ 'r' → estado 50  (return)
│   ├─ 'c' → estado 60  (class)
│   ├─ 'p' → estado 70  (public)
│   ├─ 'v' → estado 80  (void)
│   └─ outra → estado 1 (identificador genérico)
│
├─ letra maiúscula → estado 1 (identificador)
├─ '_' → estado 1 (identificador)
├─ dígito → estado 90 (número)
│
├─ '+' → estado 112  (+ ou ++)
├─ '-' → estado 113  (- ou --)
├─ '*' → TOKEN_OP_MULTIPLICACAO (imediato)
├─ '%' → TOKEN_OP_MODULO (imediato)
├─ '/' → estado 100 (divisão ou comentário)
├─ '<' → estado 101 (< ou <=)
├─ '>' → estado 102 (> ou >=)
├─ '=' → estado 103 (= ou ==)
├─ '!' → estado 104 (!= ou !)
├─ '&' → estado 105 (&& ou &)
├─ '|' → estado 106 (|| ou |)
│
├─ '(' → TOKEN_ABRE_PARENTESE (imediato)
├─ ')' → TOKEN_FECHA_PARENTESE (imediato)
├─ '{' → TOKEN_ABRE_CHAVE (imediato)
├─ '}' → TOKEN_FECHA_CHAVE (imediato)
├─ '[' → TOKEN_ABRE_COLCHETE (imediato)
├─ ']' → TOKEN_FECHA_COLCHETE (imediato)
├─ ';' → TOKEN_PONTO_VIRGULA (imediato)
├─ ',' → TOKEN_VIRGULA (imediato)
├─ '.' → TOKEN_PONTO (imediato)
├─ ':' → TOKEN_DOIS_PONTOS (imediato)
├─ '^' → TOKEN_OP_BIT_XOR (imediato)
├─ '?' → TOKEN_INTERROGACAO (imediato)
│
├─ '"' → estado 110 (string)
├─ '\'' → estado 114 (char literal)
└─ outro → TOKEN_ERRO
```

---

## Reconhecimento de Identificadores e Palavras Reservadas

### Estratégia do DFA

O lexer **não usa uma tabela hash** para palavras reservadas. Em vez disso, reconhece cada palavra reservada diretamente no autómato, estado a estado. Isto é mais eficiente e é a abordagem clássica de DFA.

### Estado 1 — Identificador Genérico

```
Estado 1:
    ler caractere
    se letra (a-z, A-Z), dígito (0-9) ou '_':
        acrescentar ao lexema
        permanecer no estado 1
    senão:
        voltarCaractere()
        retornar Token(lexema, TOKEN_IDENTIFICADOR)
```

### Reconhecimento de "if"

```
Estado 0: leu 'i' → estado 10
Estado 10: leu 'f' → estado 11
           leu 'n' → estado 30 (pode ser "int")
           outro letra/dígito → estado 1 (identificador)
           outro → voltarCaractere(), TOKEN_IDENTIFICADOR
Estado 11: leu letra/dígito → estado 1 (ex: "iffy")
           outro → voltarCaractere(), TOKEN_IF
```

### Reconhecimento de "int"

```
Estado 0: leu 'i' → estado 10
Estado 10: leu 'n' → estado 30
Estado 30: leu 't' → estado 31
           outro letra/dígito → estado 1
Estado 31: leu letra/dígito → estado 1 (ex: "integer")
           outro → voltarCaractere(), TOKEN_INT
```

### Reconhecimento de "while"

```
Estado 0: leu 'w' → estado 20
Estado 20: leu 'h' → estado 21
Estado 21: leu 'i' → estado 22
Estado 22: leu 'l' → estado 23
Estado 23: leu 'e' → estado 24
Estado 24: leu letra/dígito → estado 1
           outro → voltarCaractere(), TOKEN_WHILE
```

### Reconhecimento de "float"

```
Estado 0: leu 'f' → estado 40
Estado 40: leu 'l' → estado 41
Estado 41: leu 'o' → estado 42
Estado 42: leu 'a' → estado 43
Estado 43: leu 't' → estado 44
Estado 44: leu letra/dígito → estado 1
           outro → voltarCaractere(), TOKEN_FLOAT
```

### Reconhecimento de "return"

```
Estado 0: leu 'r' → estado 50
Estado 50: leu 'e' → estado 51
Estado 51: leu 't' → estado 52
Estado 52: leu 'u' → estado 53
Estado 53: leu 'r' → estado 54
Estado 54: leu 'n' → estado 55
Estado 55: leu letra/dígito → estado 1
           outro → voltarCaractere(), TOKEN_RETURN
```

### Reconhecimento de "class"

```
Estado 0: leu 'c' → estado 60
Estado 60: leu 'l' → estado 61
Estado 61: leu 'a' → estado 62
Estado 62: leu 's' → estado 63
Estado 63: leu 's' → estado 64
Estado 64: leu letra/dígito → estado 1
           outro → voltarCaractere(), TOKEN_CLASS
```

### Reconhecimento de "public"

```
Estado 0: leu 'p' → estado 70
Estado 70: leu 'u' → estado 71
Estado 71: leu 'b' → estado 72
Estado 72: leu 'l' → estado 73
Estado 73: leu 'i' → estado 74
Estado 74: leu 'c' → estado 75
Estado 75: leu letra/dígito → estado 1
           outro → voltarCaractere(), TOKEN_PUBLIC
```

### Reconhecimento de "void"

```
Estado 0: leu 'v' → estado 80
Estado 80: leu 'o' → estado 81
Estado 81: leu 'i' → estado 82
Estado 82: leu 'd' → estado 83
Estado 83: leu letra/dígito → estado 1
           outro → voltarCaractere(), TOKEN_VOID
```

---

## Reconhecimento de Números

### Número Inteiro (estado 90)

```
Estado 0: leu dígito → estado 90
Estado 90:
    leu dígito → acrescentar, permanecer no 90
    leu '.' e próximo é dígito → acrescentar '.', ir para estado 91
    leu '.' e próximo NÃO é dígito → voltarCaractere(), TOKEN_NUMERO_INTEIRO
    outro → voltarCaractere(), TOKEN_NUMERO_INTEIRO
```

### Número Real (estados 91 e 92)

```
Estado 91 (após ponto decimal):
    leu dígito → acrescentar, ir para estado 92
    outro → TOKEN_ERRO (ponto sem dígito depois)

Estado 92 (parte decimal):
    leu dígito → acrescentar, permanecer no 92
    leu '.' → acrescentar, TOKEN_ERRO (segundo ponto)
    outro → voltarCaractere(), TOKEN_NUMERO_REAL
```

Exemplos:
```
"42"     → TOKEN_NUMERO_INTEIRO, lexema="42"
"3.14"   → TOKEN_NUMERO_REAL,    lexema="3.14"
"3."     → TOKEN_NUMERO_INTEIRO, lexema="3"  (ponto devolvido)
"3.1.4"  → TOKEN_ERRO
```

---

## Reconhecimento de Strings

```
Estado 0: leu '"' → estado 110
Estado 110:
    leu '"' → acrescentar, TOKEN_STRING
    leu '\\' → acrescentar, ir para estado 111 (escape)
    leu '\n' ou '\0' → TOKEN_ERRO (string não fechada)
    outro → acrescentar, permanecer no 110

Estado 111 (após barra de escape):
    leu '\0' → TOKEN_ERRO
    outro → acrescentar, voltar para estado 110
```

Exemplos:
```
"hello"      → TOKEN_STRING, lexema='"hello"'
"a\nb"       → TOKEN_STRING (escape reconhecido)
"sem fechar  → TOKEN_ERRO
```

---

## Reconhecimento de Char Literals

```
Estado 0: leu '\'' → estado 114
Estado 114:
    leu '\0' ou '\n' → TOKEN_ERRO
    acrescentar caractere ao lexema
    se '\\' → ler mais um caractere (escape)
    ler próximo → deve ser '\''
    se '\'' → TOKEN_CHAR_LITERAL
    senão → TOKEN_ERRO
```

Exemplos:
```
'a'   → TOKEN_CHAR_LITERAL
'\n'  → TOKEN_CHAR_LITERAL (escape)
'ab'  → TOKEN_ERRO
```

---

## Reconhecimento de Comentários

### Comentário de linha (//)

```
Estado 0: leu '/' → estado 100
Estado 100: leu '/' → limpar lexema, ir para estado 107
Estado 107:
    leu '\n' ou '\0' → TOKEN_COMENTARIO
    outro → continuar consumindo (ignorar conteúdo)
```

### Comentário de bloco (/* ... */)

```
Estado 0: leu '/' → estado 100
Estado 100: leu '*' → limpar lexema, ir para estado 108
Estado 108:
    leu '\0' → TOKEN_ERRO (bloco não fechado)
    leu '*' → ir para estado 109 (possível fim)
    outro → permanecer no 108

Estado 109 (possível fim de bloco):
    leu '/' → TOKEN_COMENTARIO
    leu '*' → permanecer no 109
    leu '\0' → TOKEN_ERRO
    outro → voltar para estado 108
```

---

## Reconhecimento de Operadores

### Operadores de dois caracteres

```
'+' → estado 112
    leu '+' → TOKEN_OP_INCREMENTO ("++")
    outro   → voltarCaractere(), TOKEN_OP_ADICAO ("+")

'-' → estado 113
    leu '-' → TOKEN_OP_DECREMENTO ("--")
    outro   → voltarCaractere(), TOKEN_OP_SUBTRACAO ("-")

'<' → estado 101
    leu '=' → TOKEN_OP_MENOR_IGUAL ("<=")
    outro   → voltarCaractere(), TOKEN_OP_MENOR ("<")

'>' → estado 102
    leu '=' → TOKEN_OP_MAIOR_IGUAL (">=")
    outro   → voltarCaractere(), TOKEN_OP_MAIOR (">")

'=' → estado 103
    leu '=' → TOKEN_OP_IGUAL ("==")
    outro   → voltarCaractere(), TOKEN_OP_ATRIBUICAO ("=")

'!' → estado 104
    leu '=' → TOKEN_OP_DIFERENTE ("!=")
    outro   → voltarCaractere(), TOKEN_OP_NOT ("!")

'&' → estado 105
    leu '&' → TOKEN_OP_AND ("&&")
    outro   → voltarCaractere(), TOKEN_OP_BIT_AND ("&")

'|' → estado 106
    leu '|' → TOKEN_OP_OR ("||")
    outro   → voltarCaractere(), TOKEN_OP_BIT_OR ("|")
```

### Operadores de um caractere (retorno imediato)

```
'*' → TOKEN_OP_MULTIPLICACAO
'%' → TOKEN_OP_MODULO
'^' → TOKEN_OP_BIT_XOR
'?' → TOKEN_INTERROGACAO
':' → TOKEN_DOIS_PONTOS
```

---

## Métodos Auxiliares

### lerCaractere()

```java
private char lerCaractere()
```

- Lê o caractere em `conteudo[pos]` e incrementa `pos`
- Se `c == '\n'`, incrementa `linha` e reinicia `coluna`
- Se `pos >= conteudo.length`, retorna `'\0'` (fim de ficheiro)

### voltarCaractere()

```java
private void voltarCaractere()
```

- Decrementa `pos` (devolve o último caractere lido)
- Ajusta `linha` e `coluna` se o caractere devolvido era `'\n'`
- Usado quando o DFA leu um caractere a mais para decidir o estado

### peek()

```java
private char peek()
```

- Olha o próximo caractere **sem consumir** (sem alterar `pos`)
- Usado no estado 90 para decidir se o ponto é parte de um número real

### ehLetraDigitoUnderscore(char c)

```java
private boolean ehLetraDigitoUnderscore(char c)
```

- Retorna `true` se `c` é letra (a-z, A-Z), dígito (0-9) ou `_`
- Usado em todos os estados de palavras reservadas para decidir se continua como identificador

### gravarTokenLexema(String lexema, int codigoToken)

```java
private void gravarTokenLexema(String lexema, int codigoToken)
```

- Cria um `Token` e adiciona à `TabelaSimbolos` léxica
- Chamado apenas por `iniciarAnalise()` (modo autónomo)
- O parser **não usa** este método — chama `analex()` diretamente

### getNomeToken(int codigo)

```java
public static String getNomeToken(int codigo)
```

- Método estático que converte código numérico em nome legível
- Usado pelo parser e pela classe `Token.toString()`
- Exemplo: `getNomeToken(1)` → `"IDENTIFICADOR"`

### getLinhaAtual()

```java
public int getLinhaAtual()
```

- Retorna o número da linha atual
- Chamado pelo parser após cada `analex()` para registar a linha do token

---

## Classe Token

```java
public class Token {
    public String lexema;   // texto original reconhecido
    public int codigo;      // código numérico do tipo de token
    public int linha;       // linha onde foi encontrado
}
```

### Construtores

```java
Token(String lexema, int codigo)          // linha = -1
Token(String lexema, int codigo, int linha)
```

### toString()

```java
public String toString() {
    return AnalisadorLexico.getNomeToken(codigo) + ": " + lexema;
}
```

Exemplo de saída: `IDENTIFICADOR: contador`

---

## Tabela de Símbolos Léxica (lexer.TabelaSimbolos)

Esta é a tabela simples usada pelo lexer em modo autónomo. É diferente da tabela de símbolos do parser.

```java
public class TabelaSimbolos {
    private List<Token> simbolos = new ArrayList<>();

    public void adicionar(Token t)  // adiciona token à lista
    public void mostrar()           // imprime todos os tokens
}
```

Nota: esta tabela apenas guarda tokens. Não tem escopos, não tem tipos, não tem endereços. É uma lista sequencial de todos os tokens reconhecidos.

---

## Enum TipoToken

```java
public enum TipoToken {
    IDENTIFICADOR, RESERVADA, NUMERO_INTEIRO, NUMERO_REAL,
    OPERADOR_ARITMETICO, OPERADOR_RELACIONAL, OPERADOR_LOGICO,
    SIMBOLO, FIM_ARQUIVO, ERRO, COMENTARIO, LITERAL_STRING
}
```

Este enum está definido mas **não é usado** pelo `AnalisadorLexico`. O lexer usa os inteiros `TOKEN_*` diretamente. O enum existe como estrutura de apoio para categorização de alto nível.

---

## Erros Léxicos

O lexer não lança exceções. Quando encontra um caractere inválido ou uma construção mal formada, retorna um `Token` com código `TOKEN_ERRO`.

Situações que geram `TOKEN_ERRO`:

| Situação | Exemplo |
|---|---|
| Caractere não reconhecido | `@`, `#`, `$` |
| String não fechada | `"hello` |
| String com quebra de linha | `"hel\nlo"` |
| Char literal inválido | `'ab'` |
| Comentário de bloco não fechado | `/* sem fechar` |
| Número real com dois pontos | `3.1.4` |

O parser, ao receber um `TOKEN_ERRO`, regista-o como erro sintático e continua a análise.

---

## Fluxo Completo da Fase 1

```
Ficheiro fonte (.java)
        │
        ▼
AnalisadorLexico(caminho)
    └─ lê ficheiro → char[]
        │
        ▼
    analex()  ← chamado repetidamente pelo parser
        │
        ▼
    Estado 0 (inicial)
        │
        ├─ whitespace → ignorar
        ├─ letra → estados de palavras reservadas ou estado 1
        ├─ dígito → estado 90
        ├─ operador → estado específico
        ├─ delimitador → token imediato
        └─ '\0' → TOKEN_FIM_ARQUIVO
        │
        ▼
    Token(lexema, codigo, linha)
        │
        ▼
    Devolvido ao AnalisadorSintatico
```
