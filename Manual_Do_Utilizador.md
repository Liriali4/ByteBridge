# Manual do Utilizador
## Analisador Léxico - Compiladores

### 1. Introdução

Este manual destina-se a utilizadores que pretendem executar o Analisador Léxico para análise de código fonte. O analisador reconhece tokens de uma linguagem de programação simplificada, incluindo palavras reservadas, identificadores, números, operadores e comentários.

### 2. Requisitos do Sistema

- Java Development Kit (JDK) 8 ou superior
- Sistema operacional: Windows, Linux ou macOS
- Mínimo 256 MB de RAM
- 50 MB de espaço em disco

### 3. Instalação

#### 3.1. Verificar Instalação do Java

Abra o terminal/prompt de comando e execute:

```bash
java -version
```

Se o Java não estiver instalado, faça o download em: https://www.oracle.com/java/technologies/downloads/

#### 3.2. Obter o Compilador

1. Faça o download ou clone o repositório do projeto
2. Navegue até a pasta `Compilador/Compilador`

### 4. Como Usar

#### 4.1. Preparar o Arquivo de Entrada

Crie um arquivo de texto (por exemplo, `codigo.txt`) com o código fonte que deseja analisar. O arquivo deve conter código na linguagem suportada pelo analisador.

Exemplo de conteúdo válido:

```
int x = 10;
float y = 3.14;
if (x > 5) {
    return x + y;
}
while (x != 0) {
    x = x - 1;
}
```

#### 4.2. Executar o Analisador

##### Opção 1: Usando o arquivo padrão (codigo.txt)

```bash
java -cp build Main.Main
```

##### Opção 2: Especificando um arquivo

```bash
java -cp build Main.Main caminho/para/seu/arquivo.txt
```

##### Opção 3: Compilar e executar (se necessário)

```bash
# Compilar
javac -d build Compilador/src/Main/Main.java Compilador/src/lexer/*.java Compilador/src/utils/*.java

# Executar
java -cp build Main.Main codigo.txt
```

#### 4.3. Interpretar os Resultados

O analisador exibirá uma tabela com todos os tokens reconhecidos:

```
╔════════════════════════════════════════════════╗
║   ANALISADOR LÉXICO - DFA (60+ ESTADOS)       ║
║   Compiladores - Primeira Fase                 ║
╚════════════════════════════════════════════════╝

Arquivo: codigo.txt
─────────────────────────────────────────────────

╔════════════════════════════════════════════════╗
║           TABELA DE SÍMBOLOS                   ║
╚════════════════════════════════════════════════╝

INT: int
IDENTIFICADOR: x
OP_ATRIBUICAO: =
NUMERO_INTEIRO: 10
PONTO_VIRGULA: ;
...
```

### 5. Tokens Reconhecidos

#### 5.1. Palavras Reservadas
- `if`, `while`, `int`, `float`, `return`, `class`, `public`, `void`

#### 5.2. Identificadores
- Começam com letra ou underscore
- Podem conter letras, dígitos e underscores
- Exemplos: `x`, `contador`, `_temp`, `valor1`

#### 5.3. Números
- Inteiros: `0`, `123`, `9999`
- Reais: `3.14`, `0.5`, `123.456`

#### 5.4. Operadores

##### Aritméticos
- `+` (adição)
- `-` (subtração)
- `*` (multiplicação)
- `/` (divisão)
- `%` (módulo)

##### Relacionais
- `<` (menor)
- `>` (maior)
- `<=` (menor ou igual)
- `>=` (maior ou igual)
- `==` (igual)
- `!=` (diferente)

##### Lógicos
- `&&` (AND)
- `||` (OR)

##### Atribuição
- `=`

#### 5.5. Delimitadores
- `(` `)` - parênteses
- `{` `}` - chaves
- `[` `]` - colchetes
- `;` - ponto e vírgula
- `,` - vírgula
- `.` - ponto

#### 5.6. Strings
- Delimitadas por aspas duplas: `"texto"`
- Suportam caracteres de escape: `"linha1\nlinha2"`

#### 5.7. Comentários
- Linha única: `// comentário`
- Bloco: `/* comentário em múltiplas linhas */`

### 6. Tratamento de Erros

O analisador identifica os seguintes erros:

- Caracteres inválidos
- Strings não fechadas
- Comentários de bloco não fechados
- Números mal formados (ex: `3.14.5`)
- Operadores incompletos (ex: `&` sozinho)

Quando um erro é encontrado, o token é marcado como `ERRO` na tabela de símbolos.

### 7. Exemplos de Uso

#### Exemplo 1: Análise de Declarações

Arquivo `teste1.txt`:
```
int x = 5;
float pi = 3.14;
```

Saída:
```
INT: int
IDENTIFICADOR: x
OP_ATRIBUICAO: =
NUMERO_INTEIRO: 5
PONTO_VIRGULA: ;
FLOAT: float
IDENTIFICADOR: pi
OP_ATRIBUICAO: =
NUMERO_REAL: 3.14
PONTO_VIRGULA: ;
```

#### Exemplo 2: Análise de Estruturas de Controle

Arquivo `teste2.txt`:
```
if (x > 10) {
    return x;
}
```

Saída:
```
IF: if
ABRE_PARENTESE: (
IDENTIFICADOR: x
OP_MAIOR: >
NUMERO_INTEIRO: 10
FECHA_PARENTESE: )
ABRE_CHAVE: {
RETURN: return
IDENTIFICADOR: x
PONTO_VIRGULA: ;
FECHA_CHAVE: }
```

### 8. Resolução de Problemas

#### Problema: "java: command not found"
**Solução:** Instale o JDK ou adicione o Java ao PATH do sistema.

#### Problema: "Error: Could not find or load main class Main.Main"
**Solução:** Verifique se está executando o comando na pasta correta e se os arquivos .class foram compilados.

#### Problema: Arquivo não encontrado
**Solução:** Verifique o caminho do arquivo de entrada. Use caminhos relativos ou absolutos corretos.

#### Problema: Tokens não reconhecidos
**Solução:** Verifique se o código fonte está na sintaxe suportada pelo analisador.

### 9. Limitações

- O analisador reconhece apenas a sintaxe léxica, não valida a sintaxe da linguagem
- Não suporta caracteres Unicode especiais
- Strings devem estar em uma única linha (sem quebras de linha literais)
- Comentários aninhados não são suportados

### 10. Suporte

Para questões técnicas ou reportar problemas, consulte a documentação do projeto ou entre em contato com a equipe de desenvolvimento.

---

**Versão:** 2.0  
**Última atualização:** 2024
