# Manual do Programador

## Arquitetura Geral

O compilador está organizado em pacotes com responsabilidades bem definidas. Cada pacote corresponde a uma camada da arquitetura clássica de compiladores.

```
Main/           ← orquestração e ponto de entrada
lexer/          ← análise léxica (Fase 1)
parser/         ← análise sintática (Fase 2)
symbols/        ← tabela de símbolos e escopos
ast/            ← árvore sintática abstrata (preparada para Fase 3)
errors/         ← representação de erros
utils/          ← utilitários partilhados
```

---

## Pacote Main

### Main.java

Ponto de entrada do compilador.

| Método | Visibilidade | Responsabilidade |
|---|---|---|
| `main(String[])` | public static | Orquestra toda a compilação |
| `imprimirSucesso()` | private static | Imprime mensagem de sucesso |
| `imprimirFalha(AnalisadorSintatico)` | private static | Imprime lista de erros |
| `imprimirTitulo(String)` | private static | Imprime separador com título |
| `imprimirAviso(String)` | private static | Imprime aviso simples |
| `contemOpcao(String[], String)` | private static | Verifica se opção está nos args |
| `obterEntrada(String[])` | private static | Extrai ficheiro de entrada dos args |
| `resolverArquivoEntrada(String)` | private static | Resolve caminho do ficheiro |

Fluxo de `main()`:
1. Processar argumentos
2. Resolver caminho do ficheiro
3. Criar `AnalisadorLexico`
4. Criar `AnalisadorSintatico`
5. Chamar `analisarPrograma()`
6. Apresentar resultado

---

## Pacote lexer

### AnalisadorLexico.java

A classe mais extensa do projeto (1024 linhas). Implementa o DFA completo.

| Método | Visibilidade | Responsabilidade | Chamado por |
|---|---|---|---|
| `AnalisadorLexico(String)` | public | Construtor — lê ficheiro | `Main.main()` |
| `iniciarAnalise()` | public | Análise autónoma completa | Uso standalone |
| `analex()` | public | Retorna próximo token | `AnalisadorSintatico.lerToken()` |
| `lerCaractere()` | private | Lê e avança posição | `analex()` |
| `voltarCaractere()` | private | Devolve último caractere | `analex()` |
| `peek()` | private | Olha próximo sem consumir | `analex()` (estado 90) |
| `ehLetraDigitoUnderscore(char)` | private | Classifica caractere | `analex()` (estados de palavras reservadas) |
| `gravarTokenLexema(String, int)` | private | Adiciona à tabela léxica | `iniciarAnalise()` |
| `getTabelaSimbolos()` | public | Retorna tabela léxica | Uso externo |
| `getLinhaAtual()` | public | Retorna linha atual | `AnalisadorSintatico.lerToken()` |
| `getNomeToken(int)` | public static | Converte código em nome | `Token.toString()`, parser |

Atributos:
```java
private char[] conteudo    // ficheiro fonte em memória
private int pos            // posição atual
private int linha          // linha atual (começa em 1)
private int coluna         // coluna atual (começa em 1)
private TabelaSimbolos tabela  // tabela léxica
private StringBuilder lexema  // buffer do lexema atual
```

Constantes de tokens: 47 constantes `TOKEN_*` de valor 1 a 47.

### Token.java

Estrutura de dados simples.

```java
public String lexema   // texto reconhecido
public int codigo      // código TOKEN_*
public int linha       // linha (preenchida pelo parser)
```

| Método | Responsabilidade |
|---|---|
| `Token(String, int)` | Construtor sem linha (linha = -1) |
| `Token(String, int, int)` | Construtor com linha |
| `toString()` | "NOME_TOKEN: lexema" |

### TabelaSimbolos.java (lexer)

Tabela simples — lista de tokens reconhecidos.

```java
private List<Token> simbolos
```

| Método | Responsabilidade |
|---|---|
| `adicionar(Token)` | Adiciona token à lista |
| `mostrar()` | Imprime todos os tokens |

Nota: esta tabela é diferente da `symbols.TabelaSimbolos`. É usada apenas no modo autónomo do lexer.

---

## Pacote parser

### AnalisadorSintatico.java

Parser descendente recursivo. Consome tokens do lexer e constrói a tabela de símbolos.

#### Atributos

```java
private final AnalisadorLexico lexer
private final TabelaSimbolos tabelaSimbolos
private final List<ErroSintatico> listaErros
private Token tokenAtual
private Token tokenSeguinte
private int contadorBlocos
private String ultimoErro
```

#### Conjuntos de sincronização (constantes estáticas)

```java
PALAVRAS_RESERVADAS         // 31 palavras reservadas Java
SINCRONIZACAO_DECLARACAO    // tokens de sincronização para declarações
SINCRONIZACAO_BLOCO         // tokens de sincronização para blocos
SINCRONIZACAO_EXPRESSAO     // tokens de sincronização para expressões
SINCRONIZACAO_ESTRUTURA     // tokens de sincronização para estruturas de controlo
```

#### Métodos públicos

| Método | Retorna | Responsabilidade |
|---|---|---|
| `AnalisadorSintatico(AnalisadorLexico)` | — | Construtor, pré-carrega 2 tokens |
| `analisarPrograma()` | `NoAST` | Inicia análise do programa completo |
| `temErros()` | `boolean` | Verifica se há erros |
| `obterErros()` | `List<ErroSintatico>` | Retorna lista de erros |
| `obterTabelaSimbolos()` | `TabelaSimbolos` | Retorna tabela de símbolos |

#### Métodos de controlo de tokens (privados)

| Método | Retorna | Responsabilidade |
|---|---|---|
| `lerToken()` | `Token` | Pede token ao lexer, filtra comentários |
| `avancar()` | `void` | Move janela de tokens |
| `fim()` | `boolean` | Verifica TOKEN_FIM_ARQUIVO |
| `ehLexema(String)` | `boolean` | Verifica lexema do token atual |
| `ehLexemaSeguinte(String)` | `boolean` | Verifica lexema do próximo token |
| `ehIdentificador()` | `boolean` | Verifica se é identificador não reservado |
| `verificarLexema(String)` | `boolean` | Consome se lexema correto, senão false |
| `consumirLexema(String, String)` | `Token` | Consome obrigatório, com recuperação |
| `consumirIdentificador(String)` | `Token` | Consome identificador, com recuperação |
| `sintetico(String)` | `Token` | Cria token fictício para recuperação |

#### Métodos de erro e sincronização (privados)

| Método | Responsabilidade |
|---|---|
| `relatarErro(String, String)` | Regista erro, evita duplicados |
| `descrever(Token)` | Formata token para mensagem de erro |
| `sincronizar(Set<String>)` | Descarta tokens até encontrar ponto seguro |
| `sincronizarComando()` | Sincroniza em contexto de comando |
| `sincronizarDeclaracao()` | Sincroniza em contexto de declaração |
| `sincronizarExpressao()` | Sincroniza em contexto de expressão |
| `sincronizarEstrutura()` | Sincroniza em contexto de estrutura de controlo |
| `consumirFimDeclaracao(String)` | Consome `;` ou sincroniza |

#### Métodos de análise — estrutura do programa

| Método | Gramática |
|---|---|
| `analisarPrograma()` | Programa completo |
| `analisarDeclaracaoPacote()` | `package nome;` |
| `analisarDeclaracaoImportacao()` | `import nome.*;` |
| `analisarNomeQualificado(String)` | `a.b.c` |
| `analisarDeclaracaoTipo()` | Classe com modificadores |
| `analisarModificadores()` | Lista de modificadores |
| `analisarDeclaracaoClasse(List)` | `class Nome { ... }` |
| `analisarMembroClasse()` | Atributo ou método |
| `analisarRestanteMetodo(...)` | Corpo do método |
| `analisarListaParametros(Simbolo)` | `(Tipo nome, ...)` |
| `analisarParametro(Simbolo)` | `Tipo nome` |
| `analisarRestanteCampo(...)` | Declaração de atributo |
| `analisarCorpoMetodo()` | `{ comandos }` |
| `analisarBloco()` | `{ comandos }` com escopo |

#### Métodos de análise — comandos

| Método | Gramática |
|---|---|
| `analisarComando()` | Dispatcher de comandos |
| `analisarComandoSe()` | `if (cond) cmd [else cmd]` |
| `analisarComandoEnquanto()` | `while (cond) cmd` |
| `analisarComandoPara()` | `for (init; cond; upd) cmd` |
| `analisarComandoRetorno()` | `return [expr];` |
| `analisarDeclaracaoVariavelLocal(boolean)` | `Tipo nome [= expr];` |
| `declararVariavel(...)` | Cria e regista símbolo de variável |
| `analisarInicializador()` | `= expr` ou `= { ... }` |
| `declarar(Simbolo, String)` | Regista na tabela, verifica duplicados |

#### Métodos de análise — tipos

| Método | Responsabilidade |
|---|---|
| `analisarTipoOuVazio()` | Tipo ou `void` |
| `analisarTipo()` | Tipo com sufixos de array |
| `analisarSufixoArrayTipo()` | Conta `[]` no tipo |
| `analisarSufixoArrayDeclarador()` | Conta `[]` no declarador |
| `ehInicioTipo(boolean)` | Verifica se token inicia tipo |
| `ehInicioDeclaracao()` | Heurística para distinguir declaração de expressão |

#### Métodos de análise — expressões

| Método | Nível de Precedência |
|---|---|
| `analisarExpressao()` | Entrada |
| `analisarExpressaoAtribuicao()` | `= += -= ...` |
| `analisarExpressaoCondicional()` | `?:` |
| `analisarExpressaoOrLogico()` | `\|\|` |
| `analisarExpressaoELogico()` | `&&` |
| `analisarExpressaoOrBit()` | `\|` |
| `analisarExpressaoXorBit()` | `^` |
| `analisarExpressaoAndBit()` | `&` |
| `analisarExpressaoIgualdade()` | `== !=` |
| `analisarExpressaoRelacional()` | `< > <= >=` |
| `analisarExpressaoAditiva()` | `+ -` |
| `analisarExpressaoMultiplicativa()` | `* / %` |
| `analisarExpressaoUnaria()` | `+ - ! ++ --` (prefixo) |
| `analisarExpressaoPosfixa()` | `[] () . ++ --` (sufixo) |
| `analisarExpressaoPrimaria()` | Literal, identificador, `(expr)`, `new` |
| `analisarExpressaoCriacao()` | `new Tipo(...)` ou `new Tipo[...]` |
| `analisarNomeTipoCriacao()` | Tipo após `new` |
| `analisarRestanteCriacaoArray()` | `[expr][expr]...` |
| `analisarListaArgumentos()` | `expr, expr, ...` |

#### Métodos de classificação (privados)

| Método | Retorna | Responsabilidade |
|---|---|---|
| `ehLiteral()` | `boolean` | Verifica se token é literal |
| `ehTipoPrimitivo(String)` | `boolean` | Verifica tipo primitivo |
| `ehModificador(String)` | `boolean` | Verifica modificador de acesso |
| `ehOperadorAtribuicao(String)` | `boolean` | Verifica operador de atribuição composto |
| `ehPalavraReservada(String)` | `boolean` | Verifica se está em PALAVRAS_RESERVADAS |

### InformacaoTipo.java

Classe de dados para representar tipos com dimensões de array.

```java
class InformacaoTipo {
    private final String nome;      // "int", "String", "MinhaClasse"
    private final int dimensoes;    // 0, 1, 2, ...
}
```

| Método | Responsabilidade |
|---|---|
| `obterNome()` | Retorna nome base do tipo |
| `obterDimensoes()` | Retorna número de dimensões |
| `comoTexto()` | Retorna "int[][]" para nome="int", dimensoes=2 |

---

## Pacote symbols

### TabelaSimbolos.java

Gestão de escopos e símbolos durante a análise sintática.

```java
private final List<Escopo> todosEscopos  // todos os escopos criados
private Escopo escopoAtual               // escopo em uso agora
private int proximoEndereco              // próximo endereço disponível
```

| Método | Retorna | Responsabilidade | Chamado por |
|---|---|---|---|
| `TabelaSimbolos()` | — | Construtor, cria escopo global | `AnalisadorSintatico` |
| `entrarEscopo(String, String)` | `void` | Cria e entra em novo escopo | Parser ao entrar em classe/método/bloco |
| `sairEscopo()` | `void` | Volta ao escopo pai | Parser ao sair de classe/método/bloco |
| `declarar(Simbolo)` | `boolean` | Regista símbolo, false se duplicado | `AnalisadorSintatico.declarar()` |
| `resolver(String)` | `Simbolo` | Procura símbolo na cadeia de escopos | `analisarExpressaoPrimaria()` |
| `nomeEscopoAtual()` | `String` | Nome do escopo atual | Uso externo |
| `categoriaEscopoAtual()` | `String` | Categoria do escopo atual | Uso externo |
| `tamanhoSimbolo(String)` | `int` | Bytes por tipo | `declarar()`, parser |
| `imprimir()` | `void` | Imprime tabela completa | `Main.main()` com --debug |

Lógica de `declarar()`:
1. Verificar se escopo atual existe
2. Verificar se lexema já existe no escopo atual (não nos pais)
3. Definir escopo no símbolo
4. Calcular tamanho (0 para classes e métodos)
5. Atribuir endereço (ou -1 para classes/métodos)
6. Incrementar `proximoEndereco`
7. Adicionar ao escopo

### Simbolo.java

Entrada da tabela de símbolos. Todos os campos são privados com getters/setters.

Campos e seus setters:

| Campo | Setter | Tipo |
|---|---|---|
| `token` | (final, no construtor) | `String` |
| `lexema` | (final, no construtor) | `String` |
| `linha` | (final, no construtor) | `int` |
| `categoria` | (final, no construtor) | `String` |
| `tipoDado` | `definirTipoDado(String)` | `String` |
| `tipoVariavel` | `definirTipoVariavel(String)` | `String` |
| `escopo` | `definirEscopo(String)` | `String` |
| `valor` | `definirValor(String)` | `String` |
| `endereco` | `definirEndereco(int)` | `int` |
| `tamanho` | `definirTamanho(int)` | `int` |
| `inicializado` | `definirInicializado(boolean)` | `boolean` |
| `dimensoes` | `definirDimensoes(int)` | `int` |
| `parametros` | `obterParametros().add(...)` | `List<String>` |
| `modificadores` | `obterModificadores().addAll(...)` | `List<String>` |
| `tipoRetorno` | `definirTipoRetorno(String)` | `String` |

`toString()` formata todos os campos numa linha com largura fixa.

### Escopo.java

Nó da árvore de escopos.

```java
private final String nome
private final String categoria
private final Escopo pai
private final Map<String, Simbolo> simbolos  // LinkedHashMap
```

| Método | Responsabilidade |
|---|---|
| `declarar(Simbolo)` | Adiciona ao mapa, false se já existe |
| `contemNoEscopoAtual(String)` | Verifica apenas no mapa local |
| `resolver(String)` | Procura local, depois sobe para pai |
| `obterPai()` | Retorna escopo pai |
| `obterNome()` | Retorna nome |
| `obterCategoria()` | Retorna categoria |
| `obterSimbolos()` | Retorna mapa de símbolos |

---

## Pacote ast

### NoAST.java

Nó base da árvore sintática abstrata.

```java
private final String nome
private final List<NoAST> filhos
```

| Método | Responsabilidade |
|---|---|
| `adicionarFilho(NoAST)` | Adiciona filho (ignora null) |
| `obterNome()` | Retorna nome do nó |
| `obterFilhos()` | Retorna lista imutável de filhos |

### NoComando.java e NoExpressao.java

Subclasses de `NoAST` sem lógica adicional. Existem para distinguir tipos de nós na árvore.

Nota: a AST está preparada mas não é populada pelo parser atual. O parser cria apenas o nó raiz `"programa"`.

---

## Pacote errors

### ErroSintatico.java

```java
private final int linha
private final String esperado
private final String recebido
private final String contexto
```

| Método | Responsabilidade |
|---|---|
| `obterLinha()` | Retorna número da linha |
| `toString()` | Formata mensagem de erro completa |

### ExcecaoSintatica.java

Subclasse de `RuntimeException`. Não é usada no fluxo principal atual.

---

## Pacote utils

### TipoToken.java

Enum com categorias de alto nível para tokens. Não é usado pelo `AnalisadorLexico` (que usa inteiros `TOKEN_*`). Existe como estrutura de apoio.

```java
IDENTIFICADOR, RESERVADA, NUMERO_INTEIRO, NUMERO_REAL,
OPERADOR_ARITMETICO, OPERADOR_RELACIONAL, OPERADOR_LOGICO,
SIMBOLO, FIM_ARQUIVO, ERRO, COMENTARIO, LITERAL_STRING
```

---

## Interação entre Lexer e Parser

O lexer e o parser comunicam através de um protocolo simples:

```
Parser                          Lexer
  │                               │
  │── new AnalisadorLexico() ────►│ lê ficheiro para char[]
  │                               │
  │── new AnalisadorSintatico() ──┤
  │   └─ lerToken() ─────────────►│ analex() → Token
  │   └─ lerToken() ─────────────►│ analex() → Token
  │   (pré-carrega tokenAtual e tokenSeguinte)
  │                               │
  │── analisarPrograma() ─────────┤
  │   └─ avancar() ───────────────┤
  │       └─ lerToken() ─────────►│ analex() → Token
  │           (filtra comentários) │
  │           └─ getLinhaAtual() ─►│ retorna linha
  │                               │
  │   (repete até TOKEN_FIM_ARQUIVO)
```

O parser nunca acede diretamente ao `char[]` do lexer. Toda a comunicação é feita através de `analex()` e `getLinhaAtual()`.

---

## Adicionando Novas Palavras Reservadas ao Lexer

Para adicionar uma nova palavra reservada (ex: `else`):

1. Adicionar constante: `public static final int TOKEN_ELSE = 48;`
2. Adicionar ao `getNomeToken()`: `case TOKEN_ELSE: return "ELSE";`
3. No estado 0, verificar a primeira letra e ir para novo estado
4. Criar estados para cada letra da palavra
5. No estado final, verificar se o próximo caractere é letra/dígito/underscore:
   - Se sim → estado 1 (identificador)
   - Se não → `voltarCaractere()` + retornar token

---

## Adicionando Novos Comandos ao Parser

Para adicionar um novo comando (ex: `switch`):

1. Em `analisarComando()`, adicionar:
   ```java
   if (ehLexema("switch")) {
       analisarComandoSwitch();
       return;
   }
   ```
2. Criar o método `analisarComandoSwitch()` com a gramática correspondente
3. Adicionar `"switch"` a `PALAVRAS_RESERVADAS`
4. Adicionar `"switch"` aos conjuntos de sincronização relevantes

---

## Adicionando a Fase 3 — Análise Semântica

A estrutura atual já suporta a integração da Fase 3. Os passos seriam:

1. Criar pacote `semantic/`
2. Criar `AnalisadorSemantico` que recebe `TabelaSimbolos` e `NoAST`
3. Completar a construção da AST no parser (os métodos de análise devem retornar `NoAST`)
4. Implementar verificações:
   - Compatibilidade de tipos em atribuições e operações
   - Tipo de retorno de métodos
   - Variáveis usadas antes de inicialização
5. Em `Main.main()`, adicionar:
   ```java
   AnalisadorSemantico semantico = new AnalisadorSemantico(
       analisadorSintatico.obterTabelaSimbolos(),
       ast
   );
   semantico.analisar();
   ```

---

## Convenções de Código

- Métodos de análise começam com `analisar`
- Métodos de verificação começam com `eh` (retornam boolean)
- Métodos de consumo começam com `consumir`
- Métodos de sincronização começam com `sincronizar`
- Todos os métodos privados do parser têm nomes em português
- Constantes de tokens são `TOKEN_NOME_EM_MAIUSCULAS`
- Conjuntos de sincronização são `SINCRONIZACAO_CONTEXTO`
