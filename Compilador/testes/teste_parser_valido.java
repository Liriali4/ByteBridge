package exemplo.compilador;

import java.util.Scanner;

public class Programa {
    private int contador = 0;
    protected double[] valores;
    public static final boolean ativo = true;

    public static void main(String[] args) {
        int i = 0;
        int[] vetor = new int[10];
        Programa programa = new Programa();

        for (i = 0; i < 10; i++) {
            vetor[i] = i * 2;
        }

        while (i > 0) {
            i--;
        }

        if (ativo && vetor[0] >= 0) {
            programa.executar(vetor[0]);
        } else {
            return;
        }
    }

    public int executar(int valor) {
        int resultado = valor + contador;
        return resultado;
    }
}
