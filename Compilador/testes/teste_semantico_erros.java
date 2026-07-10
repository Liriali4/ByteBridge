package testes;

// Ficheiro sintaticamente valido, usado para exercitar apenas a Fase 3.
// Cada linha comentada indica a regra semantica que deve ser violada.
public class Semantica {

    int contador = 0;

    public void regras() {
        int a = 5;
        int a = 10;              // (2) variavel declarada duas vezes no mesmo escopo
        String s = "ola";
        int x = s;               // (3/4) atribuicao incompativel: int <- String
        boolean b = 1;           // (3) boolean <- int
        double d = true;         // (3) double <- boolean
        y = 3;                   // (1) variavel 'y' nao declarada
        if (a) {                 // (6) condicao do if nao e boolean
            a = a + 1;
        }
        while (contador) {       // (6) condicao do while nao e boolean
            contador = contador - 1;
        }
        for (int i = 0; i; i = i + 1) {   // (6) condicao do for nao e boolean
            a = a + i;
        }
        soma(1);                 // (5) numero de argumentos errado (espera 2)
        soma(1, s);              // (5) tipo do 2.o argumento errado (String vs int)
    }

    public int soma(int p, int q) {
        return p + q;
    }
}
