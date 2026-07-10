# Manual do Utilizador

## O que é este compilador

Este compilador processa um subconjunto da linguagem Java. Implementa três fases:

- Fase 1 — Análise Léxica: reconhece tokens (palavras, números, operadores, etc.)
- Fase 2 — Análise Sintática: verifica a estrutura do programa, constrói a tabela
  de símbolos e a árvore sintática (AST)
- Fase 3 — Análise Semântica: verifica tipos, declarações, chamadas de métodos e
  condições das estruturas de controlo

O compilador não gera código executável. O seu objetivo é verificar a correção
léxica, sintática e semântica do código fonte e produzir uma tabela de símbolos.
Os erros são apresentados numa lista única, ordenada por linha, como num
compilador real. Internamente continuam a existir erros sintáticos e semânticos,
mas essa separação não aparece no relatório final.

---

## Estrutura do Projeto

```
Compilador/
├── src/
│   ├── Main/
│   │   └── Main.java              ← ponto de entrada
│   ├── lexer/
│   │   ├── AnalisadorLexico.java  ← fase 1
│   │   ├── Token.java             ← representação de token
│   │   └── TabelaSimbolos.java    ← tabela léxica simples
│   ├── parser/
│   │   ├── AnalisadorSintatico.java ← fase 2
│   │   └── InformacaoTipo.java    ← representação de tipo
│   ├── symbols/
│   │   ├── TabelaSimbolos.java    ← tabela de símbolos completa
│   │   ├── Simbolo.java           ← entrada da tabela
│   │   └── Escopo.java            ← escopo de visibilidade
│   ├── ast/
│   │   ├── NoAST.java             ← nó base da AST
│   │   ├── NoComando.java         ← nó de comando
│   │   └── NoExpressao.java       ← nó de expressão
│   ├── errors/
│   │   ├── ErroSintatico.java     ← representação de erro
│   │   ├── ErroSemantico.java     ← erro produzido pela Fase 3
│   │   └── ExcecaoSintatica.java  ← exceção de emergência
│   ├── semantic/
│   │   └── AnalisadorSemantico.java ← fase 3
│   └── utils/
│       └── TipoToken.java         ← enum de categorias de token
├── testes/
│   ├── teste1.txt
│   ├── teste_comentarios.txt
│   ├── teste_completo.txt
│   ├── teste_demonstracao.txt
│   ├── teste_parser_bitwise_ternario.java
│   ├── teste_parser_erros.java
│   └── teste_parser_valido.java
├── build.xml                      ← build Ant (NetBeans)
└── manifest.mf
```

---

## Como Compilar o Projeto

O projeto usa Apache Ant (NetBeans). Para compilar:

```
cd Compilador
ant jar
```

Ou diretamente com javac:

```
cd Compilador/src
javac -d ../build/classes Main/Main.java lexer/*.java parser/*.java symbols/*.java ast/*.java errors/*.java utils/*.java
```

---

## Como Executar

### Sintaxe

```
java -cp build/classes Main.Main [ficheiro] [opcoes]
```

### Exemplos

Executar com o ficheiro padrão (`testes/teste_parser_erros.java`):
```
java -cp build/classes Main.Main
```

Executar com um ficheiro específico:
```
java -cp build/classes Main.Main testes/teste_parser_valido.java
```

Executar e mostrar a tabela de símbolos:
```
java -cp build/classes Main.Main testes/teste_parser_valido.java --debug
```

```
java -cp build/classes Main.Main testes/teste_parser_valido.java --tabela
```

### Opções

| Opção | Descrição |
|---|---|
| `--debug` | Mostra a tabela de símbolos completa no final |
| `--tabela` | Igual a `--debug` |

---

## Fluxo da Aplicação

Ao executar o compilador, as fases são chamadas nesta ordem:

```
Ficheiro fonte
   ↓
AnalisadorLexico
   ↓ tokens
AnalisadorSintatico
   ↓ AST + tabela de símbolos
AnalisadorSemantico
   ↓ lista de erros semânticos
Relatório final único
```

A árvore sintática abstrata (AST) é construída pelo parser e representa classes,
métodos, comandos e expressões. A Fase 3 percorre essa árvore para inferir tipos,
validar expressões e verificar comandos. A tabela de símbolos guarda classes,
métodos, atributos, parâmetros e variáveis locais; a análise semântica usa essa
tabela para consultar assinaturas de métodos e nomes de classes.

Se o parser precisar recuperar de um erro grave pelo modo pânico, pode criar nós
`erro` na AST. O analisador semântico ignora essas subárvores inválidas para não
produzir erros em cascata sobre código que já ficou sintaticamente comprometido.

Verificações semânticas implementadas:

- variáveis usadas sem declaração;
- variáveis declaradas duas vezes no mesmo escopo;
- atribuições, inicializações e retornos com tipos incompatíveis;
- chamadas de métodos com número, tipo ou ordem de argumentos incorretos;
- condições de `if`, `while`, `for` e operador ternário que não sejam `boolean`;
- operadores aplicados a tipos inválidos;
- índices de array que não sejam numéricos.

Exemplos de erros semânticos:

```
Linha 8: [variavel nao declarada] em 'x': o identificador 'x' nao foi declarado (contexto: uso de identificador)
Linha 12: [argumento incompativel] em 'somar': argumento 1 do tipo 'String' incompativel com o parametro 'int' de 'somar(int a, int b)' (contexto: chamada de metodo)
Linha 20: [condicao invalida]: a condicao de 'while' deve ser boolean, mas e 'int' (contexto: estrutura de controlo while)
```

---

## Ficheiros de Teste

### teste_parser_valido.java

Código Java sintaticamente correto. Deve compilar sem erros.

Use para verificar que o compilador aceita código válido.

### teste_parser_erros.java

Código Java com erros sintáticos intencionais. O compilador deve detetar e listar os erros.

Use para verificar o comportamento do modo pânico e a recuperação de erros.

### teste_parser_bitwise_ternario.java

Código com operadores bitwise (`&`, `|`, `^`) e operador ternário (`?:`).

Use para verificar o reconhecimento de operadores avançados. Deve compilar sem
erros sintáticos nem semânticos.

### teste_semantico_erros.java

Código **sintaticamente válido** com dez erros semânticos propositados (variável
não declarada, dupla declaração, atribuições incompatíveis, condições não-boolean
e chamadas de método com argumentos errados).

Use para verificar a Fase 3: deve produzir uma lista única com os erros
semânticos esperados, sem separar o relatório por categoria.

### teste_panico.java

Código com vários erros sintáticos espalhados por várias linhas (`;`, `)`
em falta, expressões incompletas).

Use para verificar que o modo pânico recupera de cada erro, aponta a linha
correta e não entra em ciclo infinito.

### teste_completo.txt / teste_demonstracao.txt

Ficheiros de demonstração com vários elementos da linguagem.

### teste_comentarios.txt

Ficheiro com comentários de linha (`//`) e de bloco (`/* */`).

---

## Estrutura de um Programa Válido

O compilador espera um programa com a seguinte estrutura:

```java
package nome.do.pacote;

import java.util.List;

public class NomeDaClasse {

    private int atributo;

    public int metodo(int parametro) {
        int variavel = 10;
        if (variavel > 5) {
            return variavel;
        }
        return 0;
    }
}
```

### Elementos suportados

- Declaração de pacote (`package`)
- Declarações de importação (`import`)
- Declaração de classe com modificadores (`public`, `private`, `protected`, `static`, `final`)
- Herança simples (`extends`)
- Atributos de classe com tipos primitivos e `String`
- Métodos com parâmetros e tipo de retorno
- Variáveis locais
- Arrays (`int[]`, `String[][]`)
- Comandos: `if/else`, `while`, `for`, `return`, `break`, `continue`
- Expressões com todos os operadores aritméticos, relacionais, lógicos e bitwise
- Operador ternário (`condição ? valor1 : valor2`)
- Criação de objetos (`new`)
- Acesso a atributos e chamadas de métodos encadeadas
- Literais: inteiros, reais, strings, chars, `true`, `false`, `null`
- Comentários de linha e de bloco

### Tipos primitivos suportados

`int`, `double`, `boolean`, `char`, `float`, `long`, `String`, `void`

---

## Interpretação dos Erros

Os erros são apresentados numa **lista única**, ordenada pela linha onde ocorrem.
O relatório final não separa "erros sintáticos" e "erros semânticos", embora o
compilador mantenha essas classes internamente.

```
==================================================
ERROS ENCONTRADOS
=================

[1] Linha N: [contexto]: esperado X, mas encontrado Y (TIPO)
[2] Linha M: [tipo do erro] em 'lexema': descricao (contexto: ...)

---

Compilacao terminada com 2 erro(s).

Total de erros: 2
```

### Erros de estrutura do código

Formato típico:
```
Linha N: [contexto]: esperado X [apos 'w'], mas encontrado Y (TIPO)
```

Exemplo:
```
Linha 2: [declaracao de atributo]: esperado ';' apos 'x', mas encontrado 'public' (PUBLIC)
```

| Campo | Significado |
|---|---|
| `linha N` | Linha onde o erro realmente ocorre (para `;`/`)`/`]`/`}` em falta, é a linha do fim da construção) |
| `[contexto]` | Parte da gramática onde ocorreu o erro |
| `esperado X` | O que o compilador esperava encontrar |
| `apos 'w'` | (opcional) lexema após o qual faltou o símbolo |
| `encontrado Y` | O que estava realmente no código |
| `(TIPO)` | Categoria do token encontrado |

### Erros semânticos

Formato:
```
Linha N: [tipo do erro] em 'lexema': descricao (contexto: ...)
```

Tipos de erro semântico produzidos:

| Tipo do erro | Significado |
|---|---|
| `variavel nao declarada` | Identificador usado sem ter sido declarado |
| `variavel declarada duas vezes` | Redeclaração no mesmo escopo |
| `atribuicao incompativel` | Valor incompatível com o tipo do destino (ex.: `int ← String`) |
| `condicao invalida` | Condição de `if`/`while`/`for` que não é `boolean` |
| `numero de argumentos invalido` | Chamada com número de argumentos errado |
| `argumento incompativel` | Argumento com tipo incompatível com o parâmetro |
| `metodo nao declarado` | Chamada a um método que não existe |
| `operacao invalida` | Operador aplicado a operandos de tipo errado |

### Contextos comuns (erros sintáticos)

| Contexto | Significado |
|---|---|
| `declaracao de pacote` | Erro na linha `package ...;` |
| `declaracao de importacao` | Erro na linha `import ...;` |
| `declaracao de classe` | Erro na declaração `class NomeClasse` |
| `membro da classe` | Erro num atributo ou método |
| `declaracao de metodo` | Erro na assinatura do método |
| `lista de parametros` | Erro nos parâmetros do método |
| `corpo do metodo` | Erro dentro do corpo do método |
| `declaracao de variavel local` | Erro numa variável local |
| `comando if` | Erro num `if` |
| `condicao if` | Erro na condição do `if` |
| `comando while` | Erro num `while` |
| `comando for` | Erro num `for` |
| `comando return` | Erro num `return` |
| `expressao primaria` | Erro numa expressão |
| `comando de expressao` | Erro num comando que é uma expressão |

### Erros de declaração duplicada e de identificador não declarado

Estes são erros produzidos pela Fase 3:

```
[1] Linha 8: [variavel declarada duas vezes] em 'x': o identificador 'x' ja foi declarado neste escopo (contexto: declaracao de variavel)
[2] Linha 12: [variavel nao declarada] em 'y': o identificador 'y' nao foi declarado (contexto: uso de identificador)
```

Significam, respetivamente, que `x` foi declarado duas vezes no mesmo escopo e que
`y` foi usado sem ter sido declarado.

---

## Exemplos de Código Válido

### Exemplo 1 — Classe simples

```java
package exemplo;

public class Calculadora {
    public int somar(int a, int b) {
        return a + b;
    }
}
```

### Exemplo 2 — Estruturas de controlo

```java
package exemplo;

public class Controlo {
    public int maximo(int a, int b) {
        if (a > b) {
            return a;
        } else {
            return b;
        }
    }

    public int fatorial(int n) {
        int resultado = 1;
        while (n > 1) {
            resultado = resultado * n;
            n = n - 1;
        }
        return resultado;
    }
}
```

### Exemplo 3 — Arrays e for

```java
package exemplo;

public class Arrays {
    public int soma(int[] valores) {
        int total = 0;
        for (int i = 0; i < 10; i++) {
            total = total + valores[i];
        }
        return total;
    }
}
```

### Exemplo 4 — Operador ternário e bitwise

```java
package exemplo;

public class Operadores {
    public int absoluto(int x) {
        return x >= 0 ? x : -x;
    }

    public int mascara(int valor) {
        return valor & 0xFF;
    }
}
```

---

## Exemplos de Código Inválido e Erros Esperados

### Falta de ponto e vírgula

```java
package exemplo

public class Teste { }
```

Erro esperado:
```
Linha 1: [declaracao de pacote]: esperado ';', mas encontrado 'public' (PUBLIC)
```

### Variável não declarada

```java
package exemplo;
public class Teste {
    public int metodo() {
        return x;
    }
}
```

Erro esperado:
```
Linha 4: [variavel nao declarada] em 'x': o identificador 'x' nao foi declarado (contexto: uso de identificador)
```

### Declaração duplicada

```java
package exemplo;
public class Teste {
    public int metodo(int x) {
        int x = 5;
        return x;
    }
}
```

Erro esperado:
```
Linha 4: [variavel declarada duas vezes] em 'x': o identificador 'x' ja foi declarado neste escopo (contexto: declaracao de variavel)
```

---

## Comportamento com Múltiplos Erros

O compilador não para no primeiro erro. Usa o modo pânico para recuperar e continuar a análise. Isto significa que um único ficheiro pode produzir vários erros numa só execução.

Exemplo de saída com múltiplos erros:

```
==================================================
ERROS ENCONTRADOS
=================

[1] Linha 3: [declaracao de pacote]: esperado ';', mas encontrado 'public' (PUBLIC)
[2] Linha 7: [condicao if]: esperado ')', mas encontrado '{' (ABRE_CHAVE)
[3] Linha 10: [variavel nao declarada] em 'resultado': o identificador 'resultado' nao foi declarado (contexto: uso de identificador)

---

Compilacao terminada com 3 erro(s).

Total de erros: 3
```

Nota: após um erro de sincronização, alguns erros subsequentes podem ser consequência do primeiro. Corrija os erros por ordem e recompile.
