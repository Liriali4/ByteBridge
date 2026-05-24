# Manual do Programador

## 1. Objetivo t?cnico

Documentar a implementa??o acad?mica das fases j? conclu?das:
- Fase 1 (l?xica) por m?quina de estados manual.
- Fase 2 (sint?tica) por parser descendente recursivo LL(1) pr?tico.

## 2. Arquitetura geral

- `lexer/AnalisadorLexico.java`: DFA manual, leitura de caracteres e produ??o de tokens.
- `parser/AnalisadorSintatico.java`: parsing top-down, preced?ncia de express?es, recupera??o de erros.
- `symbols/`: `TabelaSimbolos`, `Escopo`, `Simbolo`.
- `errors/ErroSintatico.java`: modelo de erro sint?tico.
- `ast/`: estrutura base para evolu??o da Fase 3.

## 3. Fase 1 - An?lise l?xica

### 3.1 Funcionamento

O m?todo `analex()` implementa um DFA com `switch(estado)`.
Fluxo:
1. `lerCaractere()` l? o pr?ximo car?cter.
2. Transi??o de estado conforme classe de car?cter.
3. Quando reconhece token final, retorna `Token`.
4. Se necess?rio, `voltarCaractere()` faz recuo de 1 car?cter.

### 3.2 Reconhecimento de tokens

Cobertura principal:
- identificadores;
- palavras reservadas j? modeladas no DFA;
- literais inteiros, reais, string, char;
- operadores aritm?ticos, relacionais, l?gicos, un?rios;
- operadores bit a bit (`&`, `|`, `^`) e tern?rio (`? :`);
- delimitadores (`(){}[];,.`);
- coment?rios de linha e bloco.

### 3.3 Erros l?xicos

`TOKEN_ERRO` ? emitido para:
- s?mbolo inv?lido;
- string n?o fechada;
- char inv?lido;
- coment?rio de bloco n?o fechado;
- forma num?rica inv?lida.

### 3.4 Tabela de s?mbolos da Fase 1

Existe tabela l?xica (`lexer/TabelaSimbolos`) para registo de tokens/lexemas reconhecidos no fluxo da fase l?xica.

## 4. Fase 2 - An?lise sint?tica

### 4.1 Estrat?gia LL(1)

- Parser descendente recursivo, sem backtracking global.
- Decis?es com 1 lookahead (`tokenAtual`) e, em pontos espec?ficos, `tokenSeguinte`.
- Gram?tica de refer?ncia: `java_grammar.txt`.

### 4.2 Recurs?o ? esquerda e fatora??o

A gram?tica de express?es est? em forma apropriada para top-down (sem recurs?o ? esquerda imediata), dividida por n?veis de preced?ncia e com produ??es opcionais/fatoradas.

### 4.3 Consumo de tokens

M?todos centrais:
- `avancar()`;
- `consumirLexema(...)`;
- `consumirIdentificador(...)`.

### 4.4 Produ??o -> m?todo do parser

Mapa principal:
- `<programa>` -> `analisarPrograma`
- `<declaracaoPacote>` -> `analisarDeclaracaoPacote`
- `<declaracaoImportacao>` -> `analisarDeclaracaoImportacao`
- `<declaracaoTipo>` / `<declaracaoClasse>` -> `analisarDeclaracaoTipo`, `analisarDeclaracaoClasse`
- `<membroClasse>` -> `analisarMembroClasse`
- `<declaracaoAtributo>` -> `analisarRestanteCampo`
- `<declaracaoMetodo>` / `<metodoPrincipal>` -> `analisarRestanteMetodo`
- `<parametros>` -> `analisarListaParametros` / `analisarParametro`
- `<corpoMetodo>` / `<bloco>` -> `analisarCorpoMetodo`, `analisarBloco`
- `<comandoIf>` -> `analisarComandoSe`
- `<comandoWhile>` -> `analisarComandoEnquanto`
- `<comandoFor>` -> `analisarComandoPara`
- `<comandoReturn>` -> `analisarComandoRetorno`
- `<declaracaoVariavelLocal>` -> `analisarDeclaracaoVariavelLocal`
- `<expressao...>` -> cadeia `analisarExpressao*`
- `<primario>`, `<criacaoObjeto>`, `<criacaoArray>` -> `analisarExpressaoPrimaria`, `analisarExpressaoCriacao`

### 4.5 Preced?ncia e associatividade

N?veis implementados (do mais baixo para o mais alto):
1. atribui??o
2. condicional `?:`
3. `||`
4. `&&`
5. `|`
6. `^`
7. `&`
8. igualdade
9. relacional
10. aditiva
11. multiplicativa
12. un?ria
13. p?s-fixa
14. prim?ria

### 4.6 Arrays, m?todos e comandos

Cobertura implementada:
- tipos com sufixo `[]`;
- declaradores com dimens?es extras;
- inicializa??o de arrays por lista `{...}`;
- cria??o com `new`;
- chamadas de m?todo, acesso a atributo, acesso a array;
- `if/else`, `while`, `for`, `return`, `break`, `continue`, blocos.

### 4.7 Modo p?nico e recupera??o

- `sincronizar(Set<String>)` avan?a at? token de sincroniza??o.
- `sincronizarComando()` usa delimitadores e in?cios de comando (`;`, `}`, `{`, `if`, `while`, `for`, `return`, `break`, `continue`).
- Recupera??o evita bloqueio em loops porque sempre h? avan?o de token durante sincroniza??o.

### 4.8 Tabela de s?mbolos expandida (Fase 2)

`Simbolo` guarda:
- token, lexema, linha, categoria
- tipoDado, tipoVariavel, escopo, valor
- endereco, tamanho, inicializado, dimensoes
- parametros, modificadores, tipoRetorno

`TabelaSimbolos`:
- controla escopos aninhados (`global`, `classe`, `metodo`, `bloco`);
- declara??es locais e resolu??o lexical por cadeia de escopos;
- base preparada para valida??es sem?nticas futuras.

## 5. Cobertura da gram?tica e limita??es

- A maioria das produ??es de `java_grammar.txt` est? implementada.
- Limita??o conhecida: `<comandoSwitch>` ? referenciado na lista de comandos, mas n?o possui defini??o completa no ficheiro de gram?tica; por isso n?o est? implementado.

## 6. Qualidade acad?mica

A implementa??o mant?m estilo acad?mico:
- classes e m?todos claros;
- sem sobre-arquitetura enterprise;
- separa??o simples de responsabilidades;
- c?digo preparado para evolu??o na Fase 3.
