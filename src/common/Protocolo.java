package common;

public class Protocolo {

    // Tipos de mensagens do protocolo
    public enum Tipo {
        DESCOBRIR,        // Broadcast para descobrir servidor
        SERVIDOR_AQUI,    // Resposta do servidor
        ENTRAR,           // Cliente pede para entrar
        ENTRADA,          // Servidor confirma entrada
        PEDIR_INICIO,     // Cliente pede início da rodada
        INICIO_RODADA,    // Servidor inicia rodada
        BATI_STOP,        // Cliente bateu STOP
        PAREM_TUDO,       // Servidor manda todos pararem
        RESPOSTAS,        // Cliente envia respostas
        RESULTADO,        // Servidor envia placar
        SAIR,             // Cliente sai do jogo
        INVALIDA          // Mensagem inválida
    }

    // Portas usadas
    public static final int PORTA_TCP = 5000;
    public static final int PORTA_UDP_DESCOBERTA = 5001;

    // Temas do jogo
    public static final String[] TEMAS = {"Nome", "Animal", "Cor", "Objeto", "Fruta"};
    public static final String SEPARADOR = ";";
}