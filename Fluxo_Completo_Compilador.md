# Fluxo Completo do Compilador

## Visão Geral

O compilador está organizado em duas fases implementadas e uma terceira preparada mas não implementada.

```
Ficheiro .java
      │
      ▼
┌─────────────────────┐
│  AnalisadorLexico   │  Fase 1 — Léxica
│  (lexer.java)       │
└─────────────────────┘
      │  Token por token (sob pedido)
      ▼
┌─────────────────────┐
│ AnalisadorSintatico │  Fase 2 — Sintática
│ (parser.java)       │
└─────────────────────┘
      │
      ├─ Lista de ErroSintatico
      └─ TabelaSimbolos (symbols)
            │
            ▼
      ┌──────────────┐
      │    Main      │  Apresentação de resultados
      └──────────────┘
```

---

## Ponto de Entrada — Main.java

### Classe: `Main` | Pacote: `Main`

### main(String[] args)

```java
public static void main(String[] args)
```

Responsabilidade: orquestrar toda a compilação.

Passos executados:

1. Verificar opções `--debug` ou `--tabela` nos argumentos
2. Determinar o ficheiro de entrada (argumento ou padrão `testes/teste_parser_erros.java`)
3. Resolver o caminho do ficheiro (tenta caminhos alternativos)
4. Criar `AnalisadorLexico` com o caminho resolvido
5. Criar `AnalisadorSintatico` passando o lexer
6. Chamar `analisadorSintatico.analisarPrograma()`
7. Verificar se há erros com `temErros()`
8. Imprimir resultado (sucesso ou lista de erros)
9. Se `--debug` ou `--tabela`, imprimir tabela de símbolos

### Resolução do Ficheiro de Entrada

```java
private static Path resolverArquivoEntrada(String entrada)
```

Tenta os seguintes caminhos por ordem:
1. Caminho exato fornecido
2. `Compilador/<entrada>`
3. `../<entrada>`

### Opções de Linha de Comando

| Opção | Efeito |
|---|---|
| (nenhuma) | Usa `testes/teste_parser_erros.java` |
| `<ficheiro>` | Usa o ficheiro especificado |
| `--debug` | Mostra tabela de símbolos no final |
| `--tabela` | Mostra tabela de símbolos no final |

---

## Fase 1 — AnalisadorLexico

### Criação

```java
AnalisadorLexico analisadorLexico = new AnalisadorLexico(caminhoArquivo.toString());
```

- Lê o ficheiro completo para memória (`char[]`)
- Inicializa posição, linha, coluna
- Cria tabela de símbolos léxica (simples)

### Uso pelo Parser

O lexer **não analisa o ficheiro todo de uma vez**. É chamado token a token pelo parser:

```java
Token lerToken() {
    do {
        tokenLido = lexer.analex();
        tokenLido.linha = lexer.getLinhaAtual();
    } while (tokenLido.codigo == TOKEN_COMENTARIO);
    ...
}
```

O parser filtra comentários automaticamente. O lexer reconhece-os mas o parser descarta-os.

---

## Fase 2 — AnalisadorSintatico

### Criação

```java
AnalisadorSintatico analisadorSintatico = new AnalisadorSintatico(analisadorLexico);
```

- Guarda referência ao lexer
- Pré-carrega dois tokens (`tokenAtual` e `tokenSeguinte`)
- Cria `TabelaSimbolos` com escopo global já criado

### Análise

```java
analisadorSintatico.analisarPrograma();
```

- Percorre toda a estrutura do programa
- Constrói a tabela de símbolos durante a análise
- Regista erros sem parar (modo pânico)

### Resultados

```java
analisadorSintatico.temErros()          // boolean
analisadorSintatico.obterErros()        // List<ErroSintatico>
analisadorSintatico.obterTabelaSimbolos() // TabelaSimbolos
```

---

## Tabela de Símbolos — Estrutura Completa

A tabela de símbolos do parser (`symbols.TabelaSimbolos`) é a estrutura central que acumula informação sobre todos os identificadores do programa.

### Organização por Escopos

```
TabelaSimbolos
    └─ List<Escopo> todosEscopos
    └─ Escopo escopoAtual
    └─ int proximoEndereco

Escopo
    └─ String nome          (ex: "global", "MinhaClasse", "calcular", "bloco1")
    └─ String categoria     (ex: "global", "classe", "metodo", "bloco")
    └─ Escopo pai           (referência ao escopo pai)
    └─ Map<String, Simbolo> simbolos
```

### Hierarquia de Escopos

```
global
  └─ MinhaClasse (classe)
       ├─ atributo1 (variavel)
       ├─ atributo2 (variavel)
       └─ calcular (metodo)
            ├─ param1 (parametro)
            ├─ param2 (parametro)
            ├─ varLocal (variavel)
            └─ bloco1 (bloco)
                 └─ varBloco (variavel)
```

### Operações da TabelaSimbolos

#### entrarEscopo(String nome, String categoria)

```java
public void entrarEscopo(String nome, String categoria)
```

- Cria novo `Escopo` com o escopo atual como pai
- Adiciona à lista `todosEscopos`
- Define como `escopoAtual`
- Chamado ao entrar em classe, método ou bloco

#### sairEscopo()

```java
public void sairEscopo()
```

- Volta ao escopo pai
- O escopo filho permanece em `todosEscopos` (para consulta posterior)
- Chamado ao sair de classe, método ou bloco

#### declarar(Simbolo simbolo)

```java
public boolean declarar(Simbolo simbolo)
```

- Verifica se o lexema já existe no escopo atual (não nos pais)
- Se já existe, retorna `false` (declaração duplicada)
- Se não existe:
  - Define o escopo do símbolo
  - Calcula e define o tamanho em bytes
  - Atribui endereço de memória (para variáveis)
  - Incrementa `proximoEndereco`
  - Adiciona ao escopo atual

#### resolver(String lexema)

```java
public Simbolo resolver(String lexema)
```

- Procura o lexema no escopo atual
- Se não encontrar, sobe para o escopo pai (recursivamente)
- Retorna `null` se não encontrar em nenhum escopo
- Usado em `analisarExpressaoPrimaria()` para verificar uso de variáveis não declaradas

#### tamanhoSimbolo(String tipo)

```java
public int tamanhoSimbolo(String tipo)
```

Calcula tamanho em bytes por tipo:

| Tipo | Bytes |
|---|---|
| `double` | 8 |
| `long` | 8 |
| `boolean` | 1 |
| `char` | 2 |
| `float` | 4 |
| `int` | 4 |
| outros (classes, String) | 4 (referência) |

---

## Classe Simbolo — Todos os Campos

```java
public class Simbolo {
    private final String token;         // nome do tipo de token (ex: "IDENTIFICADOR")
    private final String lexema;        // texto original (ex: "contador")
    private final int linha;            // linha onde foi declarado
    private final String categoria;     // "classe", "metodo", "variavel", "parametro"
    private String tipoDado;            // tipo da variável (ex: "int", "String[]")
    private String tipoVariavel;        // "local", "atributo", "parametro"
    private String escopo;              // nome do escopo onde foi declarado
    private String valor;               // valor inicial se inicializado
    private int endereco;               // endereço de memória atribuído
    private int tamanho;                // tamanho em bytes
    private boolean inicializado;       // true se tem valor inicial
    private int dimensoes;              // 0=escalar, 1=array, 2=matriz
    private List<String> parametros;    // lista de parâmetros (para métodos)
    private List<String> modificadores; // "public", "private", "static", "final"
    private String tipoRetorno;         // tipo de retorno (para métodos)
}
```

### Quando cada campo é preenchido

| Campo | Preenchido em | Quem preenche |
|---|---|---|
| `token` | Construtor | `AnalisadorSintatico` |
| `lexema` | Construtor | `AnalisadorSintatico` |
| `linha` | Construtor | `AnalisadorSintatico` |
| `categoria` | Construtor | `AnalisadorSintatico` |
| `tipoDado` | `definirTipoDado()` | `analisarRestanteMetodo`, `analisarRestanteCampo`, `declararVariavel`, `analisarParametro` |
| `tipoVariavel` | `definirTipoVariavel()` | `declararVariavel` ("local"/"atributo"), `analisarParametro` ("local") |
| `escopo` | `definirEscopo()` | `TabelaSimbolos.declarar()` |
| `valor` | `definirValor()` | `declararVariavel` (quando há `=`) |
| `endereco` | `definirEndereco()` | `TabelaSimbolos.declarar()` |
| `tamanho` | `definirTamanho()` | `TabelaSimbolos.declarar()` |
| `inicializado` | `definirInicializado()` | `declararVariavel` (quando há `=`), `analisarParametro` (sempre true) |
| `dimensoes` | `definirDimensoes()` | `declararVariavel`, `analisarParametro` |
| `parametros` | `obterParametros().add()` | `analisarParametro` |
| `modificadores` | `obterModificadores().addAll()` | `analisarRestanteMetodo`, `analisarRestanteCampo`, `analisarDeclaracaoClasse` |
| `tipoRetorno` | `definirTipoRetorno()` | `analisarRestanteMetodo` |

### Campos preenchidos na Fase 1 (lexer autónomo)

Quando o lexer é usado de forma autónoma (`iniciarAnalise()`), a tabela léxica (`lexer.TabelaSimbolos`) guarda apenas `Token` com:
- `lexema`
- `codigo`
- `linha` (sempre -1 neste modo)

### Campos preenchidos na Fase 2 (parser)

Todos os campos da classe `Simbolo` são preenchidos pelo parser.

---

## Classe Escopo

```java
public class Escopo {
    private final String nome;
    private final String categoria;
    private final Escopo pai;
    private final Map<String, Simbolo> simbolos; // LinkedHashMap (ordem de inserção)
}
```

### resolver(String lexema)

```java
public Simbolo resolver(String lexema)
```

- Procura no mapa local
- Se não encontrar, chama `pai.resolver(lexema)` recursivamente
- Implementa a regra de visibilidade: escopo interno vê o externo

---

## AST — Árvore Sintática Abstrata

As classes `NoAST`, `NoComando` e `NoExpressao` estão implementadas mas **não são usadas ativamente** pelo parser atual. O parser cria apenas um nó raiz `"programa"` em `analisarPrograma()` e não constrói a árvore completa.

```java
public class NoAST {
    private final String nome;
    private final List<NoAST> filhos;
}

public class NoComando extends NoAST { ... }
public class NoExpressao extends NoAST { ... }
```

Estas classes estão preparadas para a Fase 3 (análise semântica e geração de código).

---

## Preparação para a Fase 3

### O que já está implementado e pronto para usar

1. **Tabela de Símbolos completa** com escopos, tipos, endereços e modificadores
2. **Resolução de nomes** (`tabelaSimbolos.resolver()`) com cadeia de escopos
3. **Verificação básica de uso** de identificadores não declarados (em `analisarExpressaoPrimaria`)
4. **Estrutura AST** (`NoAST`, `NoComando`, `NoExpressao`) pronta para ser populada
5. **Informação de tipos** (`InformacaoTipo`) com suporte a arrays multidimensionais
6. **Endereços de memória** já calculados para variáveis

### O que falta implementar na Fase 3

1. **Verificação de tipos** — garantir que operações são feitas entre tipos compatíveis
2. **Verificação de retorno** — garantir que métodos não-void retornam valor
3. **Verificação de inicialização** — variáveis usadas antes de serem inicializadas
4. **Construção completa da AST** — popular a árvore durante a análise sintática
5. **Geração de código** — usar a AST e a tabela de símbolos para gerar código alvo

### Como a Fase 3 se integraria

```
AnalisadorSintatico (já existente)
    └─ produz TabelaSimbolos + AST (a completar)
        │
        ▼
AnalisadorSemantico (a criar)
    └─ recebe TabelaSimbolos e AST
    └─ verifica tipos, retornos, inicializações
    └─ anota a AST com informação de tipos
        │
        ▼
GeradorCodigo (a criar)
    └─ percorre a AST anotada
    └─ usa endereços da TabelaSimbolos
    └─ produz código alvo (bytecode, assembly, etc.)
```

---

## Saída do Compilador

### Compilação com sucesso

```
==================================================
COMPILADOR - FASE 2: ANALISADOR SINTATICO
==================================================

Arquivo: testes/teste_parser_valido.java

--------------------------------------------------

==================================================
COMPILACAO CONCLUIDA COM SUCESSO
==================================================

Analise lexica concluida.
Analise sintatica concluida.
Nenhum erro encontrado.

Total de erros: 0
```

### Compilação com erros

```
==================================================
COMPILACAO CONCLUIDA COM ERROS
==================================================

Foram encontrados 3 erros.

ERROS ENCONTRADOS
-----------------

[1] Erro Sintatico na linha 5 [declaracao de variavel local]: esperado ';', mas encontrado 'x' (IDENTIFICADOR)
[2] Erro Sintatico na linha 8 [condicao if]: esperado ')', mas encontrado '{' (ABRE_CHAVE)
[3] Erro Sintatico na linha 12 [uso de identificador]: esperado identificador declarado, mas encontrado 'y'

Total de erros: 3
```

### Tabela de Símbolos (com --debug)

```
TABELA DE SIMBOLOS
============
Escopo: global (global)
Escopo: MinhaClasse (classe)
  IDENTIFICADOR      MinhaClasse      linha=1   categoria=classe     tipoDado=MinhaClasse  ...
Escopo: calcular (metodo)
  IDENTIFICADOR      calcular         linha=3   categoria=metodo     tipoDado=int          tipoRetorno=int  ...
  IDENTIFICADOR      x                linha=3   categoria=parametro  tipoDado=int          tipoVariavel=local  ...
```
