# Explicacao completa do compilador

## 1. Ideia geral

Este compilador foi desenvolvido em Java para demonstrar as duas primeiras fases classicas de compilacao:

1. **Analise lexica:** transforma caracteres em tokens.
2. **Analise sintatica:** verifica se a sequencia de tokens respeita a gramatica da linguagem.

A linguagem aceite e inspirada em Java. O projeto reconhece classes, metodos, atributos, variaveis locais, parametros, blocos, comandos e expressoes com varios operadores.

O compilador tambem constroi uma tabela de simbolos para guardar informacoes importantes sobre os nomes declarados no programa.

## 2. Fluxo completo

O fluxo principal e:

```text
ficheiro fonte
    |
    v
AnalisadorLexico
    |
    v
tokens
    |
    v
AnalisadorSintatico
    |
    +--> validacao sintatica
    +--> recuperacao de erros
    +--> tabela de simbolos
    |
    v
relatorio final
```

O ponto de entrada esta em `Main.Main`. Ele resolve o ficheiro, cria o lexer, cria o parser e chama:

```java
analisadorSintatico.analisarPrograma();
```

No fim, imprime:

- `COMPILACAO COM SUCESSO`, quando nao ha erros;
- `COMPILACAO COM ERROS`, quando existem erros;
- total de erros;
- tabela de simbolos apenas se o utilizador usar `--debug` ou `--tabela`.

## 3. Fase 1: analisador lexico

O analisador lexico esta em `lexer.AnalisadorLexico`.

Ele le o ficheiro fonte caractere por caractere e usa uma maquina de estados finitos. Cada estado reconhece uma parte de um token. Por exemplo:

- estado inicial: decide se o proximo token comeca por letra, digito, operador ou delimitador;
- estados de identificador: continuam enquanto houver letra, digito ou `_`;
- estados de numero: distinguem inteiro e real;
- estados de string: continuam ate fechar aspas ou encontrar erro;
- estados de comentario: distinguem `//` e `/* ... */`;
- estados de operadores: distinguem `=`, `==`, `<`, `<=`, `+`, `++`, etc.

## 4. Tokens reconhecidos

O lexer reconhece:

- identificadores;
- palavras reservadas principais;
- numeros inteiros;
- numeros reais;
- strings;
- chars;
- operadores aritmeticos;
- operadores relacionais;
- operadores logicos;
- operadores bitwise;
- operador ternario `? :`;
- parenteses;
- chaves;
- colchetes;
- ponto e virgula;
- virgula;
- ponto;
- comentarios;
- fim de ficheiro;
- token de erro.

Exemplos:

```text
int       -> INT
contador  -> IDENTIFICADOR
10        -> NUMERO_INTEIRO
3.5       -> NUMERO_REAL
<=        -> OP_MENOR_IGUAL
```

## 5. Lexemas

Lexema e o texto concreto encontrado no ficheiro fonte.

No codigo:

```java
int contador = 0;
```

Temos os lexemas:

```text
int
contador
=
0
;
```

Cada lexema recebe uma classificacao, que e o token.

## 6. Erros lexicos

Quando o lexer nao consegue reconhecer corretamente um token, devolve `TOKEN_ERRO`.

Exemplos:

- string sem aspas finais;
- comentario de bloco sem `*/`;
- char mal formado;
- numero com formato invalido;
- simbolo desconhecido.

O parser regista estes erros para que tambem contem no total final.

## 7. Fase 2: analisador sintatico

O analisador sintatico esta em `parser.AnalisadorSintatico`.

Ele e um parser LL(1) implementado por descida recursiva. Isto significa que:

- a analise e feita da esquerda para a direita;
- a derivacao e mais a esquerda;
- cada metodo do parser representa uma regra da gramatica;
- o parser olha principalmente para o token atual para decidir que regra aplicar.

O parser tambem mantem `tokenSeguinte`, usado em casos simples de decisao, como diferenciar declaracao local de expressao.

## 8. Gramatica principal

A gramatica implementada pode ser resumida assim:

```text
programa          -> package nome ; importacao* declaracaoTipo*
importacao        -> import nome (. nome | . *)* ;
declaracaoTipo    -> modificadores class identificador extends? { membro* }
membro            -> modificadores tipo identificador restanteMembro
restanteMembro    -> metodo | atributo
metodo            -> ( parametros? ) { comando* }
atributo          -> declarador (, declarador)* ;
parametro         -> tipo identificador
comando           -> bloco | if | while | for | return | break | continue | declaracao | expressao
bloco             -> { comando* }
```

As expressoes seguem precedencia, do menor para o maior nivel:

```text
atribuicao
condicional ternaria
OR logico
AND logico
OR bitwise
XOR bitwise
AND bitwise
igualdade
relacional
aditiva
multiplicativa
unaria
posfixa
primaria
```

## 9. Eliminacao de recursao a esquerda

Uma gramatica LL(1) nao deve ter regras como:

```text
E -> E + T | T
```

Por isso, o parser usa repeticoes:

```text
E -> T (+ T)*
```

No codigo, isto aparece em metodos como:

- `analisarExpressaoAditiva`;
- `analisarExpressaoMultiplicativa`;
- `analisarExpressaoRelacional`;
- `analisarExpressaoIgualdade`;
- `analisarExpressaoOrLogico`;
- `analisarExpressaoELogico`.

## 10. Fatoracao

A fatoracao evita duas regras com o mesmo inicio.

Exemplo:

```text
tipo identificador ...
```

Pode ser atributo ou metodo. O parser primeiro le `tipo identificador` e depois decide:

- se vier `(`, e metodo;
- caso contrario, e atributo.

Isto esta implementado em `analisarMembroClasse()`.

## 11. Comandos reconhecidos

O parser reconhece:

- blocos `{ ... }`;
- `if` e `else`;
- `while`;
- `for`;
- `return`;
- `break`;
- `continue`;
- declaracoes de variaveis locais;
- comandos de expressao.

## 12. Expressoes reconhecidas

As expressoes suportam:

- literais;
- identificadores;
- `this`;
- `super`;
- expressoes entre parenteses;
- criacao com `new`;
- acesso a arrays;
- chamada de metodos;
- acesso por ponto;
- incremento e decremento;
- operadores unarios;
- operadores aritmeticos;
- operadores relacionais;
- operadores logicos;
- operadores bitwise;
- operador ternario;
- operadores de atribuicao.

## 13. Tabela de simbolos

A tabela de simbolos da Fase 2 fica no pacote `symbols`.

As classes principais sao:

- `TabelaSimbolos`: controla escopos e declaracoes;
- `Escopo`: guarda simbolos de um escopo especifico;
- `Simbolo`: representa uma classe, metodo, variavel ou parametro.

Cada simbolo guarda:

- token;
- lexema;
- linha;
- categoria;
- tipo de dado;
- tipo de variavel;
- escopo;
- valor;
- endereco;
- tamanho;
- inicializado;
- dimensoes;
- parametros;
- modificadores;
- tipo de retorno.

## 14. Escopos

O compilador cria escopos hierarquicos:

```text
global
  classe
    metodo
      bloco
```

A procura de nomes comeca no escopo atual e sobe para os escopos superiores. Isto permite encontrar parametros dentro de metodos e atributos dentro da classe.

Declaracoes duplicadas sao rejeitadas apenas no mesmo escopo. O mesmo nome pode aparecer em escopos diferentes.

## 15. Enderecos de memoria simulada

O campo `endereco` foi organizado para evitar colisao e repeticao indevida:

- apenas variaveis e parametros ocupam memoria simulada;
- classes e metodos usam `endereco = -1`, pois nao representam armazenamento de valor;
- o contador de memoria avanca apenas quando a declaracao e aceite;
- se houver declaracao duplicada, nao e consumido novo endereco;
- os enderecos sao sequenciais e globais.

Tamanhos:

```text
boolean -> 1 byte
char    -> 2 bytes
int     -> 4 bytes
float   -> 4 bytes
long    -> 8 bytes
double  -> 8 bytes
outros  -> 4 bytes
```

Arrays usam o tamanho do tipo base nesta fase. A quantidade real de elementos seria responsabilidade de uma fase posterior.

## 16. Tratamento de erros

O erro sintatico e representado por `errors.ErroSintatico`.

Cada mensagem informa:

- linha;
- contexto;
- token esperado;
- token encontrado.

Exemplo:

```text
Erro Sintatico na linha 6 [condicao if]: esperado ')', mas encontrado '{' (ABRE_CHAVE)
```

Tambem existem erros semanticos simples:

- declaracao duplicada;
- uso de identificador nao declarado.

## 17. Modo panico

O modo panico permite continuar a analise depois de um erro. Em vez de terminar no primeiro problema, o parser avanca ate encontrar um token seguro.

A sincronizacao foi separada por contexto:

```text
declaracoes -> ;, }, inicio de declaracao
blocos      -> }, {, inicio de comando
expressoes  -> ;, ), ], ,, }, :, {
estruturas  -> ), {, }, ;, else
```

Exemplo: se faltar `;` numa declaracao, o parser procura o fim da declaracao ou o inicio da proxima. Se faltar `)` numa condicao, procura delimitadores proprios de estruturas.

Isto reduz erros em cascata e evita loops infinitos.

## 18. Saida final

A execucao normal nao mostra a tabela de simbolos automaticamente. Isto deixa a saida mais limpa para o utilizador.

Resumo possivel sem erros:

```text
COMPILACAO COM SUCESSO
Analise lexica e sintatica concluidas sem erros.
Total de erros: 0
```

Resumo possivel com erros:

```text
COMPILACAO COM ERROS
Foram encontrados erros sintaticos/semanticos:
...
Total de erros: N
```

Para mostrar a tabela:

```text
--tabela
```

ou:

```text
--debug
```

## 19. Porque nao usar cores por defeito

Alguns consoles do NetBeans nao interpretam sequencias ANSI de cor de forma igual. Por isso, a apresentacao principal usa texto claro e portavel:

- `COMPILACAO COM SUCESSO`;
- `COMPILACAO COM ERROS`;
- `Total de erros`.

Se o ambiente suportar ANSI, cores podem ser adicionadas depois sem alterar a logica do compilador.

## 20. Estado final da Fase 1 e Fase 2

Com estas fases, o projeto consegue:

- ler um ficheiro fonte;
- reconhecer tokens;
- ignorar comentarios;
- reportar erros lexicos;
- validar a estrutura sintatica;
- reconhecer classes, metodos, variaveis, comandos e expressoes;
- criar escopos;
- preencher tabela de simbolos;
- atribuir enderecos simulados;
- recuperar de erros por modo panico;
- apresentar um resumo final adequado para defesa.
