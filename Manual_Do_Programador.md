# Manual do Programador
## Analisador Léxico - Arquitetura e Implementação

### 1. Visão Geral da Arquitetura

O Analisador Léxico é implementado como um Autômato Finito Determinístico (DFA) com mais de 60 estados, organizado em uma arquitetura modular com três componentes principais:

```
src/
├── Main/
│   └── Main.java              # Ponto de entrada da aplicação
├── lexer/
│   ├── AnalisadorLexico.java  # Motor do analisador léxico (DFA)
│   ├── Token.java             # Representação de tokens
│   └── TabelaSimbolos.java    # Gerenciamento da tabela de símbolos
└── utils/
    └── TipoToken.java         # Enumeração de tipos de tokens
```

### 2. Componentes Principais

#### 2.1. AnalisadorLexico.java

Classe central que implementa o DFA para reconhecimento de tokens.

##### Constantes de Tokens

```java
public static final int TOKEN_IDENTIFICADOR = 1;
public static final int TOKEN_IF = 2;
public static final int TOKEN_WHILE = 3;
// ... (38 tipos de tokens)
```

##### Atributos Principais

```java
private char[] conteudo;           // Buffer do código fonte
private int pos = 0;               // Posição atual no buffer
private int linha = 1;             // Linha atual
private int coluna = 1;            // Coluna atual
private TabelaSimbolos tabela;     // Tabela de símbolos
private StringBuilder lexema;      // Buffer para construção do lexema
```

##### Método Principal: analex()

O método `analex()` implementa a máquina de estados finitos:

```java
public Token analex() {
    int estado = 0;
    lexema = new StringBuilder();
    char c;
    
    while (true) {
        switch (estado) {
            case 0: // Estado inicial
                // Classificação de caracteres
            case 1: // Identificador genérico
                // Continuação de identificador
            // ... estados 10-89: palavras reservadas
            // ... estados 90-99: números
            // ... estados 100+: operadores e símbolos
        }
    }
}
```

#### 2.2. Token.java

Classe simples que encapsula um token reconhecido:

```java
public class Token {
    public String lexema;    // Texto do token
    public int codigo;       // Código do tipo de token
    
    public Token(String lexema, int codigo) {
        this.lexema = lexema;
        this.codigo = codigo;
    }
}
```

#### 2.3. TabelaSimbolos.java

Gerencia a coleção de tokens reconhecidos durante a análise.

### 3. Mapeamento de Estados do DFA

#### 3.1. Organização dos Estados

| Faixa de Estados | Função |
|-----------------|--------|
| 0-9 | Controle inicial e identificadores genéricos |
| 10-19 | Palavra reservada "if" |
| 20-29 | Palavra reservada "while" |
| 30-39 | Palavra reservada "int" |
| 40-49 | Palavra reservada "float" |
| 50-59 | Palavra reservada "return" |
| 60-69 | Palavra reservada "class" |
| 70-79 | Palavra reservada "public" |
| 80-89 | Palavra reservada "void" |
| 90-99 | Reconhecimento de números |
| 100-109 | Operadores e comentários |
| 110-111 | Strings literais |

#### 3.2. Exemplo: Reconhecimento de "while"

```
Estado 0 → lê 'w' → Estado 20
Estado 20 → lê 'h' → Estado 21
Estado 21 → lê 'i' → Estado 22
Estado 22 → lê 'l' → Estado 23
Estado 23 → lê 'e' → Estado 24
Estado 24 → verifica fim de palavra → retorna TOKEN_WHILE
```

Se em qualquer estado for lido um caractere que não corresponde à palavra reservada, o DFA transita para o estado 1 (identificador genérico).

### 4. Técnicas de Implementação

#### 4.1. Classificação por Tabela ASCII

O analisador utiliza comparações diretas com valores ASCII para eficiência:

```java
// Letras minúsculas: ASCII 97-122
if (c >= 'a' && c <= 'z') { ... }

// Letras maiúsculas: ASCII 65-90
if (c >= 'A' && c <= 'Z') { ... }

// Dígitos: ASCII 48-57
if (c >= '0' && c <= '9') { ... }

// Underscore: ASCII 95
if (c == '_') { ... }
```

#### 4.2. Lookahead e Backtracking

O analisador implementa dois mecanismos para análise contextual:

##### Peek (Lookahead)
```java
private char peek() {
    if (pos >= conteudo.length) {
        return '\0';
    }
    return conteudo[pos];
}
```

Usado para verificar o próximo caractere sem consumi-lo (ex: distinguir `3.14` de `3.`).

##### Voltar Caractere (Backtracking)
```java
private void voltarCaractere() {
    if (pos > 0) {
        pos--;
        char c = conteudo[pos];
        if (c == '\n') {
            linha--;
            coluna = 1;
        } else {
            coluna--;
        }
    }
}
```

Usado quando um caractere é lido mas não pertence ao token atual.

#### 4.3. Reconhecimento de Números

O DFA distingue entre números inteiros e reais:

```
Estado 90: Dígitos → continua em 90
          '.' seguido de dígito → Estado 91 (real)
          outro caractere → retorna NUMERO_INTEIRO

Estado 91: Primeiro dígito após '.' → Estado 92
          outro caractere → ERRO

Estado 92: Dígitos → continua em 92
          outro caractere → retorna NUMERO_REAL
```

#### 4.4. Tratamento de Comentários

##### Comentário de Linha
```java
case 107: // Comentário de linha
    c = lerCaractere();
    if (c == '\0' || c == '\n') {
        return new Token("//", TOKEN_COMENTARIO);
    }
    break;
```

##### Comentário de Bloco
```java
case 108: // Dentro do comentário
    c = lerCaractere();
    if (c == '*') {
        estado = 109; // Possível fim
    }
    break;

case 109: // Após '*'
    c = lerCaractere();
    if (c == '/') {
        return new Token("/**/", TOKEN_COMENTARIO);
    }
    break;
```

### 5. Fluxo de Execução

```
1. Main.main()
   ↓
2. new AnalisadorLexico(arquivo)
   ↓
3. analisador.iniciarAnalise()
   ↓
4. Loop: token = analex()
   ↓
5. gravarTokenLexema(token)
   ↓
6. tabela.mostrar()
```

### 6. Extensão e Manutenção

#### 6.1. Adicionar Nova Palavra Reservada

1. Definir constante em `AnalisadorLexico`:
```java
public static final int TOKEN_FOR = 39;
```

2. Alocar faixa de estados (ex: 120-129)

3. Implementar estados no switch:
```java
case 0:
    if (c == 'f') estado = 120; // for
    break;

case 120: // Leu 'f'
    c = lerCaractere();
    if (c == 'o') {
        lexema.append(c);
        estado = 121;
    } else if (ehLetraDigitoUnderscore(c)) {
        lexema.append(c);
        estado = 1;
    }
    break;

case 121: // Leu "fo"
    c = lerCaractere();
    if (c == 'r') {
        lexema.append(c);
        estado = 122;
    } else if (ehLetraDigitoUnderscore(c)) {
        lexema.append(c);
        estado = 1;
    }
    break;

case 122: // Leu "for"
    c = lerCaractere();
    if (ehLetraDigitoUnderscore(c)) {
        lexema.append(c);
        estado = 1;
    } else {
        voltarCaractere();
        return new Token("for", TOKEN_FOR);
    }
    break;
```

4. Adicionar mapeamento em `getNomeToken()`:
```java
case TOKEN_FOR: return "FOR";
```

#### 6.2. Adicionar Novo Operador

1. Definir constante:
```java
public static final int TOKEN_OP_INCREMENTO = 40;
```

2. Implementar reconhecimento no estado 0:
```java
else if (c == '+') estado = 200; // + ou ++
```

3. Criar estados para o operador:
```java
case 200: // Após '+'
    c = lerCaractere();
    if (c == '+') {
        return new Token("++", TOKEN_OP_INCREMENTO);
    } else {
        voltarCaractere();
        return new Token("+", TOKEN_OP_ADICAO);
    }
```

### 7. Otimizações Implementadas

#### 7.1. Buffer de Caracteres
- Carregamento único do arquivo em array de chars
- Acesso O(1) a qualquer posição

#### 7.2. StringBuilder para Lexemas
- Construção eficiente de strings
- Evita concatenações repetidas

#### 7.3. Classificação ASCII Direta
- Comparações numéricas em vez de métodos Character.*
- Reduz overhead de chamadas de método

### 8. Testes e Validação

#### 8.1. Casos de Teste Recomendados

```java
// Teste 1: Palavras reservadas
"if while int float return class public void"

// Teste 2: Identificadores
"x _temp contador1 CONSTANTE"

// Teste 3: Números
"0 123 3.14 0.5 999.999"

// Teste 4: Operadores
"+ - * / % < > <= >= == != = && ||"

// Teste 5: Comentários
"// linha\n/* bloco */"

// Teste 6: Strings
"\"texto\" \"com\\nescape\""

// Teste 7: Erros
"& | ! 3.14.5 \"não fechada"
```

#### 8.2. Validação de Estados

Para cada palavra reservada, verificar:
- Reconhecimento correto da palavra completa
- Transição para identificador quando há continuação (ex: "iff")
- Backtracking correto no último caractere

### 9. Considerações de Performance

- Complexidade temporal: O(n), onde n é o tamanho do arquivo
- Complexidade espacial: O(n) para buffer + O(m) para tabela de símbolos
- Cada caractere é lido exatamente uma vez (exceto em backtracking)
- Número de estados visitados por token: O(k), onde k é o tamanho do lexema

### 10. Diagrama de Classes

```
┌─────────────────────┐
│       Main          │
│  + main(String[])   │
└──────────┬──────────┘
           │ usa
           ↓
┌─────────────────────────────┐
│    AnalisadorLexico         │
│  - conteudo: char[]         │
│  - pos: int                 │
│  - linha: int               │
│  - coluna: int              │
│  - tabela: TabelaSimbolos   │
│  + analex(): Token          │
│  + iniciarAnalise(): void   │
└──────────┬──────────────────┘
           │ cria
           ↓
┌─────────────────────┐       ┌──────────────────────┐
│       Token         │       │   TabelaSimbolos     │
│  + lexema: String   │       │  + adicionar(Token)  │
│  + codigo: int      │       │  + mostrar(): void   │
└─────────────────────┘       └──────────────────────┘
```

### 11. Referências Técnicas

- Aho, A. V., Lam, M. S., Sethi, R., & Ullman, J. D. (2006). Compilers: Principles, Techniques, and Tools (2nd ed.)
- Tabela ASCII: https://www.asciitable.com/
- Teoria de Autômatos Finitos Determinísticos (DFA)

---

**Versão:** 2.0  
**Última atualização:** 2024  
**Linguagem:** Java 8+
