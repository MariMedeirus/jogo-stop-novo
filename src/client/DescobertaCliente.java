package client;

import common.Mensagem;
import common.Protocolo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class DescobertaCliente {

    public static String buscarServidor() {
        System.out.println("> Procurando servidor na rede local (broadcast UDP)...");

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(3000); // Timeout rigoroso de 3 segundos

            // Prepara a mensagem de descoberta
            Mensagem msgDescobrir = new Mensagem(Protocolo.Tipo.DESCOBRIR);
            byte[] sendData = msgDescobrir.paraLinha().getBytes();

            // Envia para o endereço de broadcast na porta 5001
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, 5001);
            socket.send(sendPacket);

            // Prepara para receber a resposta
            byte[] recvBuf = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(receivePacket);

            // Analisa a resposta recebida
            String respostaStr = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            Mensagem resposta = Mensagem.parse(respostaStr);

            if (resposta.tipo == Protocolo.Tipo.SERVIDOR_AQUI) {
                String ipServidor = receivePacket.getAddress().getHostAddress();
                System.out.println("[SUCESSO] Servidor encontrado automaticamente em: " + ipServidor);
                return ipServidor;
            }

        } catch (SocketTimeoutException e) {
            System.out.println("[TIMEOUT] Nenhum servidor respondeu após 3 segundos.");
        } catch (IOException e) {
            System.out.println("[ERRO] Falha na tentativa de descoberta UDP: " + e.getMessage());
        }

        // Retorna null se não encontrar, ativando o modo manual
        return null;
    }
}