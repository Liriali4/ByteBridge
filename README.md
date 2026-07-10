# Compilador Académico Java - Fases 1, 2 e 3

Projeto de Compiladores com:
- Fase 1: analisador léxico manual por máquina de estados.
- Fase 2: analisador sintático descendente recursivo (estilo LL(1)) com
  recuperação de erros em modo pânico, construção da tabela de símbolos e da AST.
- Fase 3: analisador semântico sobre a AST e a tabela de símbolos.

A gramática de referência da Fase 2 é `java_grammar.txt`.

## Estrutura

- `Compilador/src/lexer`: léxico.
- `Compilador/src/parser`: parser e apoio de tipos sintáticos.
- `Compilador/src/symbols`: tabela de símbolos com escopos.
- `Compilador/src/ast`: nós da árvore sintática (AST), construída na Fase 2.
- `Compilador/src/semantic`: analisador semântico (Fase 3).
- `Compilador/src/errors`: modelos de erro (léxico/sintático/semântico).
- `Compilador/src/Main/Main.java`: ponto de entrada.

## Compilar

```bash
javac -d Compilador/build/classes $(find Compilador/src -name '*.java')
```

## Executar

Exemplo válido (0 erros):

```bash
java -cp Compilador/build/classes Main.Main Compilador/testes/teste_parser_valido.java
```

Exemplo com erros semânticos (Fase 3):

```bash
java -cp Compilador/build/classes Main.Main Compilador/testes/teste_semantico_erros.java
```

Exemplo com erros sintáticos (modo pânico):

```bash
java -cp Compilador/build/classes Main.Main Compilador/testes/teste_panico.java
```

Use `--tabela` (ou `--debug`) para ver a tabela de símbolos.

## Estado atual

- Parsing top-down por métodos recursivos, com precedência/associatividade completas.
- Modo pânico corrigido: o erro é registado antes de sincronizar e a linha
  apresentada é sempre a real (símbolos terminadores em falta apontam ao fim da
  construção). Sincronização específica por contexto e proteção contra ciclos.
- Tabela de símbolos por escopo (`global`, `classe`, `método`, `bloco`) com
  consultas de apoio à análise semântica.
- AST real construída durante o parsing.
- Fase 3: variável não declarada, dupla declaração, incompatibilidade de tipos,
  atribuições/argumentos incompatíveis e condições de controlo `boolean`.

## Limitações conhecidas

- A gramática menciona `comandoSwitch`, `do while` e `foreach`, mas não estão
  ativos nesta fase (o analisador semântico já está preparado para as condições).
- Tipos de atributos de outras classes (`obj.campo`) não são inferidos (evita
  falsos positivos); é um ponto natural de evolução.
- Não há verificação de uso-antes-de-inicialização nem de retorno obrigatório.
- **Nota sobre o léxico:** o analisador léxico não termina se o ficheiro não
  acabar com mudança de linha. Como não pode ser alterado, o `Main` normaliza a
  entrada (garante um `\n` final) antes de o invocar.

## Documentação

- `explicando.md` — visão geral das três fases.
- `explicando_fase01_lexer.md` — Fase 1.
- `explicando_fase02_parser.md` — Fase 2 (parser, modo pânico, AST).
- `explicando_fase03_semantico.md` — Fase 3 (análise semântica).
- `modo_panico_explicando.md` — modo pânico em detalhe.
- `Fluxo_Completo_Compilador.md` — fluxo completo.
- `Manual_Do_Programador.md` / `Manual_Do_Utilizador.md`.
- `RELATORIO_VALIDACAO_FASE03.md` — relatório de validação final.
