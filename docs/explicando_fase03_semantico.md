# Fase 3 — Analisador Semântico

## Objetivo da Fase

A análise sintática (Fase 2) garante que o programa respeita a **estrutura** da
linguagem, mas não verifica se ele faz **sentido**. Um programa como

```java
int x = "texto";
boolean b = 3;
soma(1, 2, 3);   // método declarado com 2 parâmetros
```

é perfeitamente válido do ponto de vista gramatical, mas está semanticamente
errado. A Fase 3 é responsável por detetar exatamente este tipo de problemas.

O analisador semântico é uma **camada totalmente separada** do parser. Recebe
dois produtos já construídos na Fase 2 e não volta a ler tokens nem reconstrói a
estrutura do programa:

1. a **Árvore Sintática Abstrata (AST)** produzida pelo `AnalisadorSintatico`;
2. a **Tabela de Símbolos** preenchida durante a análise sintática.

```
AnalisadorLexico  ──►  AnalisadorSintatico  ──►  AnalisadorSemantico
   (tokens)             (AST + Tabela)            (verificações + ErroSemantico)
```

Cada fase tem responsabilidades bem definidas e nenhuma faz o trabalho da outra.
O parser **não** verifica tipos nem declarações; o analisador semântico **não**
verifica gramática nem constrói a árvore.

---

## Classes Envolvidas

| Classe | Pacote | Papel na Fase 3 |
|--------|--------|-----------------|
| `AnalisadorSemantico` | `semantic` | Percorre a AST e aplica todas as regras semânticas. |
| `ErroSemantico` | `errors` | Representa um erro semântico (separado de `ErroSintatico`). |
| `NoAST` / `NoComando` / `NoExpressao` | `ast` | Nós da árvore percorrida; guardam nome, lexema, linha e tipo. |
| `TabelaSimbolos` | `symbols` | Consultada para assinaturas de métodos e nomes de classes. |
| `Simbolo` | `symbols` | Fornece tipo de retorno, parâmetros e assinatura de métodos. |

A separação dos três tipos de erro (`ErroLexico`, `ErroSintatico`,
`ErroSemantico`) foi feita de propósito: cada fase produz o seu próprio tipo,
para que nunca haja confusão entre categorias no relatório final.

---

## A Árvore Sintática Abstrata (AST)

Na Fase 2, o parser deixou de devolver apenas um nó vazio: agora **constrói uma
árvore real** enquanto reconhece o programa. Cada nó (`NoAST`) guarda:

- `nome` — a categoria do nó (`"classe"`, `"binario"`, `"identificador"`, …);
- `lexema` — o texto relevante (nome da variável, operador, valor do literal);
- `linha` — a linha do código fonte (usada nas mensagens de erro);
- `tipo` — preenchido pelo analisador semântico com o tipo inferido;
- `filhos` — as sub-árvores.

### Principais categorias de nós

**Estruturais**

| Nó | Significado | Filhos |
|----|-------------|--------|
| `programa` | Raiz da árvore | nós `classe` |
| `classe` | Declaração de classe | atributos e métodos |
| `atributo` | Campo da classe | (opcional) inicializador |
| `metodo` | Método | parâmetros, depois comandos do corpo |
| `parametro` | Parâmetro formal | — |

**Comandos** (`NoComando`)

`declaracoes`, `declaracaoLocal`, `comandoExpressao`, `se`, `enquanto`, `para`,
`retorno`, `break`, `continue`, `bloco`, `vazio`.

**Expressões** (`NoExpressao`)

`literalInt`, `literalReal`, `literalString`, `literalChar`, `literalBool`,
`literalNull`, `identificador`, `binario`, `unario`, `atribuicao`, `ternario`,
`chamada`, `acessoArray`, `acessoMembro`, `posIncremento`, `novoObjeto`,
`novoArray`, `erro`.

### Exemplo real

O comando `int z = (x & y) ^ (x | y);` (do ficheiro
`teste_parser_bitwise_ternario.java`) produz a seguinte sub-árvore:

```
declaracoes
└─ declaracaoLocal 'z'  (tipo declarado = int)
   └─ binario '^'
      ├─ binario '&'
      │  ├─ identificador 'x'
      │  └─ identificador 'y'
      └─ binario '|'
         ├─ identificador 'x'
         └─ identificador 'y'
```

O analisador semântico percorre esta árvore de baixo para cima: infere que `x` e
`y` são `int`, que `&`, `|` e `^` sobre inteiros dão `int`, e finalmente que é
legítimo atribuir `int` a uma variável `int`.

---

## Estrutura do AnalisadorSemantico

### Atributos

```java
private final NoAST raiz;                       // raiz da AST
private final TabelaSimbolos tabela;            // tabela da Fase 2
private final List<ErroSemantico> erros;        // relatório acumulado
private final Deque<Map<String,String>> escopos;// pilha de escopos (nome -> tipo)
private final Set<String> nomesDeClasses;       // classes conhecidas
private String tipoRetornoAtual;                // tipo de retorno do método atual
```

O ponto central é a **pilha de escopos**. Cada escopo é um mapa
`nome → tipo declarado`. Ao entrar numa classe, método, bloco ou `for`, empilha-se
um novo mapa; ao sair, desempilha-se. A resolução de um nome percorre a pilha do
topo (escopo mais interno) para a base (escopo global). É este mecanismo que
suporta a **visibilidade** e a **procura em escopos superiores**.

> **Porque uma pilha própria e não a `TabelaSimbolos`?**
> Depois da análise sintática, o escopo "atual" da tabela já regressou ao global
> (todos os `sairEscopo()` foram executados). Reconstruir a hierarquia de escopos
> locais a partir da tabela seria complicado. Por isso o analisador semântico
> mantém a sua própria pilha, alimentada pela AST, e usa a tabela apenas para o
> que é global à travessia: **assinaturas de métodos** e **nomes de classes**.
> Assim reutiliza-se a tabela sem duplicar a informação de tipos.

---

## Sequência de Chamadas dos Métodos

```
analisar()
├─ recolherNomesDeClasses()          (lê a tabela uma vez)
├─ entrarEscopo()                    (escopo global)
└─ para cada classe:
   analisarClasse(no)
   ├─ entrarEscopo()                 (escopo da classe)
   ├─ 1.º passo: declara todos os atributos
   ├─ 2.º passo:
   │  ├─ verificarInicializacao(atributo)
   │  └─ analisarMetodo(metodo)
   │     ├─ entrarEscopo()           (escopo do método)
   │     ├─ declara parâmetros
   │     ├─ para cada comando: analisarComando(...)
   │     └─ sairEscopo()
   └─ sairEscopo()
```

O `analisarClasse` faz **dois passos** sobre os membros: primeiro regista todos
os atributos, só depois processa métodos e inicializações. Assim um método pode
referenciar um campo declarado mais abaixo no código, tal como em Java.

### analisarComando(no)

Distribui cada comando pela verificação adequada:

| Nó | Verificação |
|----|-------------|
| `bloco` | entra num novo escopo, processa os filhos, sai do escopo |
| `declaracoes` / `declaracaoLocal` | declara a variável (deteta redeclaração) e verifica a inicialização |
| `comandoExpressao` | infere o tipo da expressão (dispara as restantes verificações) |
| `se` / `enquanto` | verifica que a condição é `boolean`; processa o(s) corpo(s) |
| `para` | novo escopo; verifica condição `boolean`; processa init, atualização e corpo |
| `retorno` | verifica que o valor devolvido é compatível com o tipo de retorno |
| `break` / `continue` / `vazio` | nada a verificar |

### tipoDe(no)

É o coração da inferência de tipos. Recebe um nó de expressão, calcula o seu
tipo, **anota-o no próprio nó** (`no.definirTipo(...)`) e devolve-o. Durante o
cálculo dispara as verificações de variáveis não declaradas, operações
inválidas, atribuições incompatíveis e argumentos de métodos.

---

## As Seis Verificações Exigidas

### 1. Variável não declarada

Quando `tipoDe` encontra um nó `identificador`, procura-o na pilha de escopos.
Se não existir (e não for o nome de uma classe), regista o erro:

```
Erro na linha 16 [variavel nao declarada] em 'y': o identificador 'y' nao foi declarado
```

### 2. Variável declarada duas vezes no mesmo escopo

Ao declarar uma variável local ou parâmetro, verifica-se se o nome **já existe
no escopo atual** (não nos exteriores — redeclarar num bloco interno é legítimo):

```
Erro na linha 11 [variavel declarada duas vezes] em 'a': o identificador 'a' ja foi declarado neste escopo
```

### 3 e 4. Incompatibilidade de tipos / atribuições incompatíveis

A função `compativelAtribuicao(destino, origem)` decide se um valor do tipo
`origem` pode ser atribuído a um destino do tipo `destino`. As regras são:

- tipos iguais → compatível;
- `null` → compatível apenas com tipos referência (`String`, arrays, classes);
- alargamento numérico permitido segundo a ordem
  `char → int → long → float → double` (nunca o contrário, que seria estreitamento);
- tudo o resto → **incompatível**.

Assim, `int ← String`, `boolean ← int` e `double ← boolean` são todos rejeitados:

```
Erro na linha 13 [atribuicao incompativel] em 'x': nao e possivel atribuir 'String' a variavel do tipo 'int'
Erro na linha 14 [atribuicao incompativel] em 'b': nao e possivel atribuir 'int' a variavel do tipo 'boolean'
Erro na linha 15 [atribuicao incompativel] em 'd': nao e possivel atribuir 'boolean' a variavel do tipo 'double'
```

A mesma função valida atribuições (`x = ...`), inicializações (`int x = ...`),
elementos de arrays e valores de `return`.

### 5. Compatibilidade dos argumentos de métodos

Numa chamada `nome(args)`, procura-se o método na tabela
(`tabela.procurarMetodo(nome)`) e comparam-se, por `validarArgumentos`:

- **quantidade** de argumentos vs. parâmetros;
- **tipo** de cada argumento vs. o tipo do parâmetro na mesma **posição**
  (usando as mesmas regras de compatibilidade), o que verifica também a **ordem**.

```
Erro na linha 26 [numero de argumentos invalido] em 'soma': o metodo 'int soma(int, int)' espera 2 argumento(s), mas recebeu 1
Erro na linha 27 [argumento incompativel] em 'soma': argumento 2 do tipo 'String' incompativel com o parametro 'int' de 'int soma(int, int)'
```

### 6. Estruturas de controlo

As condições de `if`, `while`, `for` e do operador ternário têm de ser `boolean`.
O método `verificarCondicao` infere o tipo da condição e, se não for `boolean`
(nem desconhecido), regista:

```
Erro na linha 17 [condicao invalida]: a condicao de 'if' deve ser boolean, mas e 'int'
```

O `do while`, o `foreach` e o `switch` estão previstos na gramática/documentação
mas ainda não são gerados pelo parser; o `verificarCondicao` já está preparado
para os cobrir quando forem implementados.

---

## Não Parar no Primeiro Erro

O analisador semântico **nunca lança exceções** para interromper a análise.
Todos os erros são acumulados na lista `erros` e a travessia continua. No fim,
`analisar()` devolve o relatório completo.

Para evitar **erros em cascata**, os tipos que resultam de um erro anterior são
representados por um tipo especial `"desconhecido"`. A função
`compativelAtribuicao` (e as verificações de operadores) tratam `"desconhecido"`
como compatível com tudo, de modo que um único erro não gera dezenas de erros
derivados. Exemplo: se `y` não está declarada, `y + 1` fica `desconhecido` e a
atribuição `x = y + 1` não gera um segundo erro de tipos.

---

## Cada Erro Informa

Conforme exigido, cada `ErroSemantico` transporta:

| Campo | Exemplo |
|-------|---------|
| **linha** | `16` |
| **lexema** | `y` |
| **tipo do erro** | `variavel nao declarada` |
| **descrição** | `o identificador 'y' nao foi declarado` |
| **contexto** | `uso de identificador` |

Formato final:

```
Erro na linha 16 [variavel nao declarada] em 'y': o identificador 'y' nao foi declarado (contexto: uso de identificador)
```

---

## Interação com o Parser e a Tabela de Símbolos

- **Reutiliza a AST**: não reprocessa tokens; percorre a árvore já construída.
- **Reutiliza a Tabela**: usa `procurarMetodo`, `metodoExiste` e a lista de
  classes para resolver chamadas e referências a tipos, sem reconstruir estas
  estruturas.
- **Não duplica informação**: os tipos das variáveis locais vêm da própria AST
  (dos nós de declaração), e as assinaturas de métodos vêm da tabela. Cada dado
  tem uma única fonte.

A `TabelaSimbolos` foi reforçada na Fase 2 com métodos de consulta pensados para
esta fase: `existe`, `declaradoNoEscopoAtual`, `declaradoEmEscopoSuperior`,
`tipoDe`, `estaInicializada`, `procurarMetodo`, `metodoExiste` e `obterEscopos`.

---

## Exemplo Completo (ficheiro `teste_semantico_erros.java`)

Entrada (sintaticamente válida, dez erros semânticos propositados):

```java
public class Semantica {
    int contador = 0;
    public void regras() {
        int a = 5;
        int a = 10;              // (2) declarada duas vezes
        String s = "ola";
        int x = s;               // (3/4) int <- String
        boolean b = 1;           // (3) boolean <- int
        double d = true;         // (3) double <- boolean
        y = 3;                   // (1) y nao declarada
        if (a) { a = a + 1; }    // (6) condicao nao boolean
        while (contador) { contador = contador - 1; }  // (6)
        for (int i = 0; i; i = i + 1) { a = a + i; }    // (6)
        soma(1);                 // (5) numero de argumentos
        soma(1, s);              // (5) tipo do argumento 2
    }
    public int soma(int p, int q) { return p + q; }
}
```

Saída da Fase 3:

```
ERROS SEMANTICOS (10)
-----------------
[1]  linha 11 [variavel declarada duas vezes] em 'a'
[2]  linha 13 [atribuicao incompativel] em 'x'   : int <- String
[3]  linha 14 [atribuicao incompativel] em 'b'   : boolean <- int
[4]  linha 15 [atribuicao incompativel] em 'd'   : double <- boolean
[5]  linha 16 [variavel nao declarada] em 'y'
[6]  linha 17 [condicao invalida]                : if deve ser boolean, mas e int
[7]  linha 20 [condicao invalida]                : while deve ser boolean, mas e int
[8]  linha 23 [condicao invalida]                : for deve ser boolean, mas e int
[9]  linha 26 [numero de argumentos invalido] em 'soma'
[10] linha 27 [argumento incompativel] em 'soma'
```

As dez violações são detetadas numa única execução, cada uma na sua linha
correta, e o programa `teste_parser_valido.java` continua a produzir **zero**
erros semânticos.

---

## Limitações Atuais e Evolução Futura

- Os tipos de **atributos de outras classes** (`obj.campo`) não são inferidos:
  `acessoMembro` devolve `desconhecido` para não gerar falsos positivos. É um
  ponto natural de evolução (guardar a tabela de campos por classe).
- Não há verificação de **uso antes de inicialização** (a tabela já regista a
  flag `inicializado`; falta a travessia de fluxo).
- Não há verificação de **retorno obrigatório** em todos os caminhos de um método
  não-`void`.
- `do while`, `foreach` e `switch` ficam preparados mas por implementar.

A arquitetura foi pensada para crescer: novas regras entram como novos métodos de
verificação, sem alterar o parser nem a tabela.
