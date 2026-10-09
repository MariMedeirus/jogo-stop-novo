package client;

import common.Protocolo;

public class EstadoCliente {

    // Estados possíveis do cliente
    public enum Estado {
        LOBBY,                // Aguardando no lobby
        EM_RODADA,            // Jogando uma rodada
        AGUARDANDO_RESULTADO  // Aguardando pontuação
    }

    private volatile Estado estado = Estado.LOBBY;
    private char letra;

    private String[] palavras = new String[Protocolo.TEMAS.length];
    private int indicePreenchimento = 0;

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public char getLetra() {
        return letra;
    }

    public void setLetra(char letra) {
        this.letra = letra;
    }

    public synchronized int getIndicePreenchimento() {
        return indicePreenchimento;
    }

    public synchronized void avancarIndice() {
        this.indicePreenchimento++;
    }

    public synchronized void setPalavra(int index, String palavra) {
        if (index >= 0 && index < palavras.length) {
            palavras[index] = palavra;
        }
    }

    public synchronized String[] getPalavras() {
        return palavras.clone();
    }

    // Reseta dados para nova rodada
    public synchronized void limparRodada() {
        this.indicePreenchimento = 0;
        for (int i = 0; i < palavras.length; i++) {
            palavras[i] = "";
        }
    }
}
