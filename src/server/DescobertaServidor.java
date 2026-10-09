package server;

import common.Mensagem;
import common.Protocolo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

// Thread UDP que responde a quem pergunta onde o servidor esta
public class DescobertaServidor implements Runnable {

    // Porta UDP onde esta thread escuta
    private final int portaUdp;
    // Porta TCP do jogo, informada na resposta
    private final int portaTcp;

    public DescobertaServidor(int portaUdp, int portaTcp) {
        this.portaUdp = portaUdp;
        this.portaTcp = portaTcp;
    }

    @Override
    public void run() {
        // Sem endereco, o socket escuta em todas as interfaces e recebe broadcast
        try (DatagramSocket socket = new DatagramSocket(portaUdp)) {
            log("Descoberta UDP ouvindo na porta " + portaUdp);

            byte[] buffer = new byte[1024];

            while (true) {
                // Pacote novo a cada volta, pois o receive encolhe o pacote
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

                try {
                    // Bloqueia ate chegar um datagrama
                    socket.receive(pacote);
                    tratar(socket, pacote);
                } catch (Exception e) {
                    // Datagrama estranho nao pode derrubar a thread
                    if (socket.isClosed()) {
                        break;
                    }
                    log("Datagrama ignorado - " + e.getMessage());
                }
            }

        } catch (SocketException e) {
            // Porta ocupada desliga so a descoberta e o jogo TCP continua
            log("Descoberta UDP indisponivel - " + e.getMessage());
        }
    }

    private void tratar(DatagramSocket socket, DatagramPacket pacote) throws IOException {
        // Converte so os bytes recebidos, o resto do buffer e lixo
        String texto = new String(pacote.getData(), 0, pacote.getLength(),
                StandardCharsets.UTF_8).trim();

        Mensagem m = Mensagem.parse(texto);
        // Responde somente a mensagens do tipo DESCOBRIR
        if (m.tipo != Protocolo.Tipo.DESCOBRIR) {
            log("Datagrama ignorado de " + pacote.getAddress() + " - " + texto);
            return;
        }

        // Em UDP a resposta vai para o endereco e a porta de origem do pacote
        InetAddress origem = pacote.getAddress();
        int portaOrigem = pacote.getPort();

        // Monta SERVIDOR_AQUI com o IP e a porta TCP
        String ip = descobrirIpLocal(origem, portaOrigem);
        String resposta = "SERVIDOR_AQUI" + Protocolo.SEPARADOR + ip
                + Protocolo.SEPARADOR + portaTcp;

        byte[] dados = resposta.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(dados, dados.length, origem, portaOrigem));

        log("Respondi a " + origem.getHostAddress() + " - " + resposta);
    }

    // Descobre o IP nosso que o cliente consegue alcancar
    private String descobrirIpLocal(InetAddress cliente, int portaCliente) {
        // O connect em UDP nao envia nada e so fixa o destino do socket
        try (DatagramSocket temporario = new DatagramSocket()) {
            temporario.connect(cliente, portaCliente);
            return temporario.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            // Plano B quando o metodo acima falha
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e2) {
                return "127.0.0.1";
            }
        }
    }

    private void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }
}