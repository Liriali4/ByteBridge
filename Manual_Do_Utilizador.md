# Manual do Utilizador

## O que é este compilador

Este compilador processa um subconjunto da linguagem Java. Atualmente implementa duas fases:

- Fase 1 — Análise Léxica: reconhece tokens (palavras, números, operadores, etc.)
- Fase 2 — Análise Sintática: verifica a estrutura do programa e constrói a tabela de símbolos

O compilador não gera código executável. O seu objetivo é verificar a correção léxica e sintática do código fonte e produzir uma tabela de símbolos.

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
│   │   └── ExcecaoSintatica.java  ← exceção de emergência
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

## Ficheiros de Teste

### teste_parser_valido.java

Código Java sintaticamente correto. Deve compilar sem erros.

Use para verificar que o compilador aceita código válido.

### teste_parser_erros.java

Código Java com erros sintáticos intencionais. O compilador deve detetar e listar os erros.

Use para verificar o comportamento do modo pânico e a recuperação de erros.

### teste_parser_bitwise_ternario.java

Código com operadores bitwise (`&`, `|`, `^`) e operador ternário (`?:`).

Use para verificar o reconhecimento de operadores avançados.

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

### Formato de um erro

```
Erro Sintatico na linha N [contexto]: esperado X, mas encontrado Y (TIPO)
```

Exemplo:
```
Erro Sintatico na linha 5 [declaracao de variavel local]: esperado ';', mas encontrado 'x' (IDENTIFICADOR)
```

### Campos do erro

| Campo | Significado |
|---|---|
| `linha N` | Número da linha onde o erro foi detetado |
| `[contexto]` | Parte da gramática onde ocorreu o erro |
| `esperado X` | O que o compilador esperava encontrar |
| `encontrado Y` | O que estava realmente no código |
| `(TIPO)` | Categoria do token encontrado |

### Contextos comuns

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
| `uso de identificador` | Variável usada sem ter sido declarada |
| `declaracao de classe duplicada` | Nome de classe já declarado |

### Erros de declaração duplicada

```
Erro Sintatico na linha 8 [declaracao de variavel local duplicada]: esperado declaracao unica, mas encontrado 'x'
```

Significa que o identificador `x` já foi declarado no mesmo escopo.

### Erros de identificador não declarado

```
Erro Sintatico na linha 12 [uso de identificador]: esperado identificador declarado, mas encontrado 'y'
```

Significa que `y` foi usado numa expressão mas nunca foi declarado. Nota: identificadores que começam com maiúscula (nomes de classes) não são verificados.

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
Erro Sintatico na linha 1 [declaracao de pacote]: esperado ';', mas encontrado 'public' (PUBLIC)
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
Erro Sintatico na linha 4 [uso de identificador]: esperado identificador declarado, mas encontrado 'x'
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
Erro Sintatico na linha 4 [declaracao de variavel local duplicada]: esperado declaracao unica, mas encontrado 'x'
```

---

## Comportamento com Múltiplos Erros

O compilador não para no primeiro erro. Usa o modo pânico para recuperar e continuar a análise. Isto significa que um único ficheiro pode produzir vários erros numa só execução.

Exemplo de saída com múltiplos erros:

```
==================================================
COMPILACAO CONCLUIDA COM ERROS
==================================================

Foram encontrados 3 erros.

ERROS ENCONTRADOS
-----------------

[1] Erro Sintatico na linha 3 [declaracao de pacote]: esperado ';', mas encontrado 'public' (PUBLIC)
[2] Erro Sintatico na linha 7 [condicao if]: esperado ')', mas encontrado '{' (ABRE_CHAVE)
[3] Erro Sintatico na linha 10 [uso de identificador]: esperado identificador declarado, mas encontrado 'resultado'

Total de erros: 3
```

Nota: após um erro de sincronização, alguns erros subsequentes podem ser consequência do primeiro. Corrija os erros por ordem e recompile.
