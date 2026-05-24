# Explicação Completa do Projeto Compilador (Fases 1 e 2)

## 1. Objetivo do projeto
Este projeto implementa um compilador acadêmico em Java dividido em duas fases principais:

1. Fase léxica: transforma texto-fonte em tokens.
2. Fase sintática: valida a estrutura gramatical, recupera erros e constrói/atualiza tabela de símbolos por escopo.

O objetivo não é gerar código final nesta fase, mas sim validar corretamente programas no subconjunto de Java adotado e produzir diagnóstico de erros úteis.

## 2. Estrutura de pastas e responsabilidade
- `Compilador/src/Main/Main.java`: ponto de entrada e orquestração.
- `Compilador/src/lexer`: analisador léxico e token.
- `Compilador/src/parser`: analisador sintático LL(1)-like e metadados de tipo.
- `Compilador/src/symbols`: tabela de símbolos com escopos aninhados.
- `Compilador/src/errors`: modelagem de erro sintático.
- `Compilador/src/ast`: nós-base para evolução de AST (fase futura).
- `Compilador/testes`: ficheiros de entrada para validação do compilador.

## 3. Fluxo global de execução
Fluxo em alto nível:

1. `Main.main(args)` resolve o caminho do ficheiro de entrada.
2. `Main` instancia `AnalisadorLexico`.
3. `Main` instancia `AnalisadorSintatico` com o léxico.
4. Parser chama `analisarPrograma()`.
5. Parser consome tokens via `lexer.analex()` (com lookahead de 1 token).
6. Durante parsing, símbolos são declarados/resolvidos por escopo.
7. Erros são acumulados em lista (não para no primeiro erro).
8. No fim, `Main` imprime erros (se houver) e imprime tabela de símbolos.

## 4. Fase de entrada e robustez de ficheiros (`Main.java`)
Arquivo: `Compilador/src/Main/Main.java`

### 4.1 Responsabilidades do `Main`
- Definir ficheiro padrão (`ARQUIVO_PADRAO`).
- Aceitar ficheiro por argumento (`args[0]`).
- Resolver caminhos relativos em diferentes diretórios de execução.
- Tratar falta de ficheiro com mensagem amigável.
- Disparar análise léxica/sintática e impressão de resultados.

### 4.2 Fluxo de `main(String[] args)`
1. Escolhe entrada:
- com argumento: usa argumento.
- sem argumento: usa `ARQUIVO_PADRAO`.

2. Resolve caminho com `resolverArquivoEntrada`.

3. Se não encontrar ficheiro:
- imprime mensagem amigável.
- encerra sem stacktrace.

4. Se encontrar:
- cria `AnalisadorLexico`.
- cria `AnalisadorSintatico`.
- executa `analisarPrograma()`.

5. Resultado:
- se `temErros()`: imprime todos os erros.
- senão: imprime sucesso.

6. Imprime tabela de símbolos final.

### 4.3 Fluxo de `resolverArquivoEntrada(String entrada)`
Tenta, por ordem:
1. caminho informado diretamente.
2. `Compilador/<entrada>`.
3. `../<entrada>`.

Retorna `Path` normalizado quando encontra; `null` se nenhum existir.

## 5. Fase léxica (`lexer`)

## 5.1 Classe `Token`
Arquivo: `Compilador/src/lexer/Token.java`

Representa uma unidade léxica com:
- `lexema`: texto reconhecido.
- `codigo`: tipo/token id.
- `linha`: linha no arquivo.

`toString()` converte `codigo` para nome via `AnalisadorLexico.getNomeToken`.

## 5.2 Classe `lexer.TabelaSimbolos`
Arquivo: `Compilador/src/lexer/TabelaSimbolos.java`

É uma tabela simples de tokens reconhecidos na análise léxica:
- `adicionar(Token t)`: guarda token.
- `mostrar()`: imprime tokens guardados.

Observação: esta tabela é diferente da tabela semântica/sintática em `symbols.TabelaSimbolos`.

## 5.3 Classe `AnalisadorLexico`
Arquivo: `Compilador/src/lexer/AnalisadorLexico.java`

### 5.3.1 Papel
Implementa scanner por DFA (autômato finito determinístico) com estados explícitos, reconhecendo:
- palavras reservadas
- identificadores
- números inteiros e reais
- operadores aritméticos, relacionais, lógicos e bitwise
- pontuação/símbolos
- literais string e char
- comentários de linha e bloco

### 5.3.2 Estado interno relevante
- `conteudo[]`: buffer do arquivo fonte.
- `pos`: posição atual.
- `linha`, `coluna`: controle de posição.
- `lexema`: acumulador do token atual.

### 5.3.3 Métodos principais
1. `AnalisadorLexico(String caminho)`
- carrega bytes do arquivo.
- inicializa buffer e estrutura de apoio.

2. `analex()`
- método central: roda DFA até produzir 1 token.
- ignora whitespaces.
- retorna `TOKEN_FIM_ARQUIVO` ao final.
- pula comentários no parser (não aqui).

3. `iniciarAnalise()`
- varre todos os tokens até EOF.
- grava em tabela léxica e imprime.
- é útil para debug/etapa léxica isolada.

4. Auxiliares:
- `lerCaractere()`: consome 1 char e atualiza linha/coluna.
- `voltarCaractere()`: faz 1-char rollback.
- `peek()`: lookahead de caractere sem consumir.
- `ehLetraDigitoUnderscore(char c)`: valida charset de identificador.
- `gravarTokenLexema(...)`: armazena token reconhecido.
- `getLinhaAtual()`: usado pelo parser para marcar linha.
- `getNomeToken(int codigo)`: mapeamento id -> nome textual.

### 5.3.4 Lógica do DFA (resumo por grupos de estados)
1. Estado `0` (inicial):
- decide para qual família ir: identificador/reservada, número, operador, delimitador, string/char.

2. Estados de identificador e reservadas:
- `1`: identificador genérico.
- `10-11`: `if`.
- `20-24`: `while`.
- `30-31`: `int`.
- `40-44`: `float`.
- `50-55`: `return`.
- `60+`: `class`.
- `70+`: `public`.
- `80+`: `void`.

3. Estados numéricos:
- `90`: inteiro.
- `91-92`: real após ponto decimal.

4. Estados operadores/comentários:
- `100`: `/`, `//`, `/*`.
- `101`: `<` ou `<=`.
- `102`: `>` ou `>=`.
- `103`: `=` ou `==`.
- `104`: `!` ou `!=`.
- `105`: `&` ou `&&`.
- `106`: `|` ou `||`.
- `107`: comentário de linha.
- `108-109`: comentário de bloco.

5. Estados literais:
- `110-111`: string e escape.
- `114`: char (inclui escape).

6. Incremento/decremento:
- `112`: `+` ou `++`.
- `113`: `-` ou `--`.

## 6. Fase sintática e semântica básica (`parser`)

## 6.1 Classe `InformacaoTipo`
Arquivo: `Compilador/src/parser/InformacaoTipo.java`

Encapsula tipo com:
- `nome` (ex.: `int`, `Programa`, `String`).
- `dimensoes` (quantidade de `[]`).

`comoTexto()` devolve tipo final textual (ex.: `int[][]`).

## 6.2 Classe `AnalisadorSintatico`
Arquivo: `Compilador/src/parser/AnalisadorSintatico.java`

### 6.2.1 Papel
Parser descendente recursivo com:
- lookahead de 1 token (`tokenAtual`, `tokenSeguinte`).
- recuperação de erro por sincronização.
- integração com tabela de símbolos e validação de identificador declarado.

### 6.2.2 Estado interno
- `lexer`: origem dos tokens.
- `tabelaSimbolos`: escopos e declarações.
- `listaErros`: erros acumulados.
- `tokenAtual` e `tokenSeguinte`: janela de lookahead.
- `contadorBlocos`: nomear escopos de bloco.
- `PALAVRAS_RESERVADAS`: evita tratar reservada como identificador.

### 6.2.3 Métodos de infraestrutura (núcleo do parser)
1. `lerToken()`
- chama `lexer.analex()`.
- copia linha atual.
- ignora tokens `COMENTARIO`.

2. `avancar()`
- desloca janela de lookahead.

3. `fim()`
- verifica EOF.

4. Predicados:
- `ehLexema`, `ehLexemaSeguinte`, `ehIdentificador`, `verificarLexema`.

5. Consumidores com diagnóstico:
- `consumirLexema(...)`
- `consumirIdentificador(...)`

6. Erros e recuperação:
- `relatarErro(...)`
- `descrever(...)`
- `sincronizar(...)`
- `sincronizarComando()`

### 6.2.4 Entrada da gramática
#### `analisarPrograma()`
Fluxo:
1. exige `package` no início.
2. aceita zero ou mais `import`.
3. aceita zero ou mais declarações de tipo até EOF.

Retorna nó AST raiz (`NoAST("programa")`), ainda sem popular árvore detalhada.

### 6.2.5 Declarações de topo
1. `analisarDeclaracaoPacote()`
- consome `package`, nome qualificado, `;`.

2. `analisarDeclaracaoImportacao()`
- consome `import`, nome qualificado com possível `.*`, `;`.

3. `analisarNomeQualificado(...)`
- lê identificadores separados por `.` e devolve string final.

4. `analisarDeclaracaoTipo()`
- lê modificadores.
- exige `class`.
- caso contrário, relata erro e sincroniza.

### 6.2.6 Classe e membros
1. `analisarModificadores()`
- coleta `public/private/protected/static/final`.

2. `analisarDeclaracaoClasse(modificadores)`
- consome `class Nome`.
- declara símbolo de classe.
- opcional `extends`.
- abre escopo da classe.
- analisa membros até `}`.
- fecha escopo.

3. `analisarMembroClasse()`
- decide entre método e atributo:
  - lê tipo e nome.
  - se próximo token é `(`: método.
  - senão: campo.

4. `analisarRestanteMetodo(...)`
- cria símbolo `metodo`.
- define retorno e metadados.
- declara método.
- entra escopo do método.
- analisa parâmetros e corpo.
- sai escopo do método.

5. `analisarListaParametros(metodo)`
- lê parâmetros separados por vírgula até `)`.

6. `analisarParametro(metodo)`
- lê tipo + identificador.
- adiciona assinatura textual ao método.
- declara parâmetro no escopo local do método.

7. `analisarRestanteCampo(...)`
- declara 1 ou mais atributos do mesmo tipo.
- aceita inicialização e arrays no declarador.
- exige `;`.

### 6.2.7 Blocos e comandos
1. `analisarCorpoMetodo()`
- consome `{ ... }` com comandos.

2. `analisarBloco()`
- abre escopo `blocoN`, analisa comandos, fecha escopo.

3. `analisarComando()`
Despacho por primeiro token:
- bloco `{...}`
- `if`
- `while`
- `for`
- `return`
- `break;`
- `continue;`
- declaração local
- expressão seguida de `;`

4. `analisarComandoSe()`
- `if (expr) comando [else comando]`

5. `analisarComandoEnquanto()`
- `while (expr) comando`

6. `analisarComandoPara()`
- `for (init; cond; update) comando`
- `init` pode ser declaração local sem `;` interno extra.

7. `analisarComandoRetorno()`
- `return [expr];`

### 6.2.8 Declarações locais e inicialização
1. `analisarDeclaracaoVariavelLocal(consumirPontoEVirgula)`
- tipo + uma ou mais variáveis separadas por vírgula.
- decide se consome `;` conforme contexto (normal/for-init).

2. `declararVariavel(...)`
- trata sufixo `[]` no declarador.
- monta `InformacaoTipo` final.
- cria `Simbolo`, preenche metadados.
- processa inicializador opcional.
- registra na tabela.

3. `analisarInicializador()`
- suporta `{...}` para array initializer.
- senão analisa expressão normal.

4. `declarar(...)`
- delega à `TabelaSimbolos`.
- se duplicado no mesmo escopo, registra erro.

### 6.2.9 Tipos
1. `analisarTipoOuVazio()`
- aceita `void` ou tipo normal.

2. `analisarTipo()`
- primitivo, `String`, ou nome qualificado.
- consome dimensões de array no tipo.

3. `analisarSufixoArrayTipo()`
- conta `[]` no tipo base.

4. `analisarSufixoArrayDeclarador()`
- conta `[]` no nome declarador.

5. `ehInicioTipo(...)`, `ehInicioDeclaracao()`
- heurísticas de decisão sintática.

### 6.2.10 Expressões (precedência)
Cadeia de precedência implementada:

1. `analisarExpressao()` -> atribuição.
2. `analisarExpressaoAtribuicao()`
3. `analisarExpressaoCondicional()` (`?:`)
4. `analisarExpressaoOrLogico()` (`||`)
5. `analisarExpressaoELogico()` (`&&`)
6. `analisarExpressaoOrBit()` (`|`)
7. `analisarExpressaoXorBit()` (`^`)
8. `analisarExpressaoAndBit()` (`&`)
9. `analisarExpressaoIgualdade()` (`==`, `!=`)
10. `analisarExpressaoRelacional()` (`<`, `>`, `<=`, `>=`)
11. `analisarExpressaoAditiva()` (`+`, `-`)
12. `analisarExpressaoMultiplicativa()` (`*`, `/`, `%`)
13. `analisarExpressaoUnaria()` (prefix `+ - ! ++ --`)
14. `analisarExpressaoPosfixa()` (`[]`, chamada `()`, acesso `.`, `++ --`)
15. `analisarExpressaoPrimaria()` (literal, id, `this`, `super`, parentizada, `new`)

### 6.2.11 Criação (`new`)
1. `analisarExpressaoCriacao()`
- decide entre construção de objeto e array.

2. `analisarNomeTipoCriacao()`
- tipo primitivo/String ou nome qualificado.

3. `analisarRestanteCriacaoArray()`
- valida sintaxe de dimensões de array.

### 6.2.12 Verificações semânticas básicas
Em `analisarExpressaoPrimaria()`:
- ao usar identificador, tenta resolver na tabela.
- se não existe e não começa com maiúscula, registra erro de identificador não declarado.

Isto evita confundir nomes de classes com variáveis comuns.

### 6.2.13 Predicados utilitários
- `ehLiteral()`
- `ehTipoPrimitivo(...)`
- `ehModificador(...)`
- `ehOperadorAtribuicao(...)`
- `ehPalavraReservada(...)`

## 7. Tabela de símbolos semântica (`symbols`)

## 7.1 Classe `Simbolo`
Arquivo: `Compilador/src/symbols/Simbolo.java`

Modela uma entrada de símbolo com metadados ricos:
- identidade: token, lexema, linha, categoria.
- tipagem: tipo de dado, tipo de variável, dimensões, tipo de retorno.
- memória aproximada: endereço, tamanho.
- estado: inicializado, valor textual.
- assinatura: parâmetros.
- modificadores: `public`, `static`, etc.

## 7.2 Classe `Escopo`
Arquivo: `Compilador/src/symbols/Escopo.java`

Representa escopo com:
- nome e categoria (`global`, `classe`, `metodo`, `bloco`).
- ponteiro para escopo pai.
- mapa ordenado de símbolos locais.

Métodos:
- `declarar`: insere símbolo se não existir localmente.
- `resolver`: busca local, depois sobe para pai.

## 7.3 Classe `TabelaSimbolos`
Arquivo: `Compilador/src/symbols/TabelaSimbolos.java`

### Fluxo principal
1. construtor abre escopo `global`.
2. `entrarEscopo(...)` cria escopo filho e torna atual.
3. `sairEscopo()` volta para pai.
4. `declarar(Simbolo)`:
- define escopo, endereço e tamanho.
- incrementa ponteiro de próximo endereço.
- delega inserção para escopo atual.
5. `resolver(...)`: lookup lexical em cadeia.
6. `imprimir()`: mostra todos os escopos/símbolos.

### Heurística de tamanho (`tamanhoSimbolo`)
- `double/long`: 8
- `boolean`: 1
- `char`: 2
- `float/int`: 4
- default: 4

## 8. Modelo de erros (`errors`)

## 8.1 `ErroSintatico`
Arquivo: `Compilador/src/errors/ErroSintatico.java`

Carrega:
- linha
- esperado
- recebido
- contexto

`toString()` formata mensagem amigável para output final.

## 8.2 `ExcecaoSintatica`
Arquivo: `Compilador/src/errors/ExcecaoSintatica.java`

Exceção runtime disponível para cenários de erro sintático fatal.
No fluxo atual, o parser prioriza acumular erros em lista ao invés de lançar exceção para cada falha.

## 9. AST base (`ast`)

## 9.1 `NoAST`
Arquivo: `Compilador/src/ast/NoAST.java`

Nó genérico com:
- `nome`
- lista de `filhos`
- operações para adicionar e consultar filhos

## 9.2 `NoComando` e `NoExpressao`
Arquivos:
- `Compilador/src/ast/NoComando.java`
- `Compilador/src/ast/NoExpressao.java`

Especializações simples de `NoAST` para evolução futura da árvore sintática.
Na fase atual, parser ainda não monta AST detalhada por produção.

## 10. Como os testes se conectam ao fluxo
Pasta: `Compilador/testes`

Arquivos principais:
- `teste_parser_valido.java`: cenário sem erros.
- `teste_parser_erros.java`: cenário com erros para testar recuperação.
- `teste_parser_bitwise_ternario.java`: operadores bitwise e condicional.

Execução típica:
1. sem argumento: usa arquivo padrão configurado no `Main`.
2. com argumento: usa caminho fornecido.

Exemplo:
`java -cp Compilador/build/classes Main.Main testes/teste_parser_valido.java`

## 11. Estratégia de recuperação de erros
O parser segue princípio de continuidade:

1. registra erro com contexto.
2. não aborta imediatamente.
3. sincroniza em tokens seguros (`;`, `}`, inícios de comando).
4. retoma parsing para reportar múltiplos erros numa execução.

Benefício: ótimo para uso didático, pois mostra conjunto de falhas de uma vez.

## 12. Limites atuais e pontos de evolução
Estado atual é forte para Fase 2, porém ainda há espaço:

1. AST completa por produção ainda não está sendo construída.
2. Verificações semânticas são básicas (escopo/declaração), sem sistema de tipos completo.
3. Léxico usa constantes numéricas de token; poderia migrar para enum central.
4. `AnalisadorLexico` imprime stacktrace ao falhar leitura de arquivo; pode ser refinado para erro amigável consistente com `Main`.

## 13. Como explicar o projeto numa apresentação
Roteiro curto que funciona bem:

1. Problema: validar um subconjunto de Java em duas fases.
2. Arquitetura: `Main` -> `Lexer` -> `Parser` -> `Tabela de Símbolos`.
3. Léxico: DFA por estados para tokenização robusta.
4. Sintático: recursivo com precedência de expressões e recuperação de erro.
5. Símbolos: escopos aninhados com declaração/resolução.
6. Resultado: erros legíveis + tabela final de símbolos.
7. Demonstração: rodar arquivo válido e depois arquivo com erros.

## 14. Mapa rápido de funções por fase

### Entrada/execução
- `Main.main`
- `Main.resolverArquivoEntrada`

### Léxico
- `AnalisadorLexico.analex`
- `AnalisadorLexico.lerCaractere`
- `AnalisadorLexico.voltarCaractere`
- `AnalisadorLexico.peek`
- `AnalisadorLexico.getNomeToken`

### Sintático
- `AnalisadorSintatico.analisarPrograma`
- família de `analisarDeclaracao...`
- família de `analisarComando...`
- família de `analisarExpressao...`
- `AnalisadorSintatico.declarar`
- `AnalisadorSintatico.sincronizar`

### Símbolos
- `TabelaSimbolos.entrarEscopo`
- `TabelaSimbolos.sairEscopo`
- `TabelaSimbolos.declarar`
- `TabelaSimbolos.resolver`

## 15. Resumo final
Este projeto está bem estruturado para ensino de compiladores:

1. separa claramente fases léxica, sintática e gestão de símbolos;
2. implementa parsing por precedência de forma legível;
3. mantém robustez via recuperação de erros;
4. permite demonstrar tanto sucesso quanto falhas com casos de teste.

Com este entendimento, você consegue explicar arquitetura, decisões de implementação e fluxo de execução como alguém que escreveu o sistema de ponta a ponta.
