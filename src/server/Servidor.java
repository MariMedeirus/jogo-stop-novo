package server;

import common.Protocolo;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Thread principal do servidor: só aceita conexões e entrega cada cliente
 * a um ClienteHandler, que roda em thread própria.
 */
public class Servidor {

    public static void main(String[] args) {

        // Objeto Partida compartilhado por todos os handlers
        Partida partida = new Partida();

        // Abre a porta TCP e começa a escutar
        try (ServerSocket serverSocket = new ServerSocket(Protocolo.PORTA_TCP)) {
            System.out.println("Servidor ouvindo na porta " + Protocolo.PORTA_TCP);

            // Descoberta UDP roda em paralelo ao jogo
            Thread descoberta = new Thread(
                    new DescobertaServidor(Protocolo.PORTA_UDP_DESCOBERTA, Protocolo.PORTA_TCP),
                    "descoberta-udp");
            // Daemon para nao impedir o programa de encerrar
            descoberta.setDaemon(true);
            descoberta.start();

            while (true) {
                // Espera um cliente conectar
                Socket socket = serverSocket.accept();
                System.out.println("Conexão aceita de " + socket.getRemoteSocketAddress());

                // um handler e uma thread para cada cliente
                ClienteHandler handler = new ClienteHandler(socket, partida);
                Thread thread = new Thread(handler, "handler-" + socket.getRemoteSocketAddress());

                // start() roda o handler
                thread.start();
                // Volta ao accept() para o próximo cliente
            }

        } catch (IOException e) {
            // porta ocupada ou erro fatal no ServerSocket
            System.out.println("Erro no servidor: " + e.getMessage());
        }
    }
}