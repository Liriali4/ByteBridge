# Manual do Utilizador

## 1. Vis?o geral

Este projeto implementa duas fases do compilador acad?mico:
- Fase 1: Analisador L?xico.
- Fase 2: Analisador Sint?tico (descendente recursivo, estilo LL(1)) com recupera??o de erros.

A gram?tica oficial da Fase 2 est? em `java_grammar.txt`.

## 2. Estrutura de ficheiros relevante

- `Compilador/src/lexer`: implementa??o do lexer.
- `Compilador/src/parser`: implementa??o do parser.
- `Compilador/src/symbols`: tabela de s?mbolos por escopo.
- `Compilador/src/errors`: modelo de erros sint?ticos.
- `Compilador/src/Main/Main.java`: ponto de entrada atual (pipeline l?xico + sint?tico).
- `teste_parser_valido.java`: caso v?lido.
- `teste_parser_erros.java`: caso inv?lido para validar recupera??o.

## 3. Como compilar

Na raiz do projeto:

```bash
javac -d Compilador/build/classes Compilador/src/Main/Main.java Compilador/src/lexer/*.java Compilador/src/utils/*.java Compilador/src/parser/*.java Compilador/src/symbols/*.java Compilador/src/errors/*.java Compilador/src/ast/*.java
```

## 4. Como executar

### 4.1 Execu??o completa (Fase 1 + Fase 2)

```bash
java -cp Compilador/build/classes Main.Main <ficheiro>
```

Exemplos:

```bash
java -cp Compilador/build/classes Main.Main teste_parser_valido.java
java -cp Compilador/build/classes Main.Main teste_parser_erros.java
```

## 5. Formato de entrada

- C?digo Java no subconjunto definido por `java_grammar.txt`.
- A produ??o `<programa>` exige `package` no in?cio.

Exemplo v?lido curto:

```java
package exemplo;
public class A {
    public int soma(int a, int b) {
        int c = a + b;
        return c;
    }
}
```

Exemplo inv?lido curto:

```java
public class A {
    int x
    public void m() {
        y = 10;
    }
}
```

## 6. Formato de sa?da

A execu??o imprime:
- cabe?alho de execu??o;
- resultado da an?lise sint?tica (sucesso ou lista de erros);
- tabela de s?mbolos por escopo.

## 7. Interpreta??o de erros

### 7.1 Erros l?xicos

O lexer emite `TOKEN_ERRO` para lexemas inv?lidos (por exemplo, string mal fechada, s?mbolo inv?lido).
Esses tokens s?o propagados para o parser e acabam reportados como erro sint?tico no contexto onde aparecem.

### 7.2 Erros sint?ticos

Formato padr?o:

```text
Erro Sintatico na linha X [contexto]: esperado <...>, mas encontrado <...>
```

O parser tenta recuperar com modo p?nico para continuar a an?lise e reportar m?ltiplos erros.

## 8. Estrutura dos testes

- `teste_parser_valido.java`: valida fluxo normal sem erros.
- `teste_parser_erros.java`: valida dete??o de erros + recupera??o.
- Podem ser criados ficheiros adicionais na raiz e executados com o mesmo comando do `Main`.

## 9. Observa??es pr?ticas

- Se aparecerem muitos erros, come?ar pelos primeiros da lista.
- O projeto est? preparado para evolu??o para Fase 3 (sem?ntica), mas sem?ntica ainda n?o est? implementada.
