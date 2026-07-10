package testes;

public class Panico {

    int x                       // falta ';' -> erro deve apontar a esta linha

    public void metodo(int a {  // falta ')' na lista de parametros
        int b = ;               // expressao em falta
        if (a > 0) {
            b = a +             // expressao incompleta antes do ';'
        }
        while (a < 10 {         // falta ')' na condicao
            a = a + 1;
        }
        return b
    }

    public int outro() {
        return 0;
    }
}
