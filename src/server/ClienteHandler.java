package server;

import common.Mensagem;
import common.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ClienteHandler implements Runnable {
    private final Socket socket;
    private final Partida partida;
    private BufferedReader in;
    private PrintWriter out;
    private volatile Jogador jogador;   // null até o ENTRAR ser aceito

    public ClienteHandler(Socket socket, Partida partida) {
        this.socket = socket;
        this.partida = partida;
    }

    public Socket getSocket() {
        return socket;
    }

    public Partida getPartida() {
        return partida;
    }

    public Jogador getJogador() {
        return jogador;
    }

    // Envia mensagem ao cliente
    public synchronized void enviar(String msg) {
        out.println(msg);
    }

    @Override
    public void run() {
        try {
            // Inicializa streams de comunicação
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Loop: lê mensagens do cliente
            String linha;
            while ((linha = in.readLine()) != null) {
                tratar(linha);
            }
        } catch (IOException e) {
            log("Conexão encerrada: " + e.getMessage());
        } finally {
            limpar();
        }
    }

    // Remove jogador e fecha conexão
    private void limpar() {
        if (jogador != null) {
            partida.removerJogador(jogador.getId());
            log("Jogador saiu: " + jogador);
        }
        try {
            socket.close();
        } catch (IOException e) {
            // já estava fechado
        }
    }

    // Processa mensagem recebida do cliente
    private void tratar(String linha) {
        linha = linha.trim();
        if (linha.isEmpty()) {
            return;
        }

        Mensagem m = Mensagem.parse(linha);

        switch (m.tipo) {
            case ENTRAR:
                tratarEntrar(m.campos);
                break;
            case PEDIR_INICIO:
                tratarPedirInicio();
                break;
            case BATI_STOP:
                tratarBatiStop();
                break;
            case RESPOSTAS:
                tratarRespostas(m.campos);
                break;
            case SAIR:
                tratarSair();
                break;
            default:
                log("Mensagem não tratada ainda (tipo=" + m.tipo + "): " + linha);
                break;
        }
    }

    // Processa pedido de entrada do jogador
    private void tratarEntrar(String[] campos) {
        if (jogador != null) {
            erroEntrada("Ja_conectado");
            return;
        }
        if (campos.length != 1) {
            erroEntrada("Formato_invalido");
            return;
        }

        String nome = campos[0].trim();

        if (nome.isEmpty()) {
            erroEntrada("Nome_vazio");
            return;
        }
        if (nome.contains("#") || nome.contains(":")) {
            erroEntrada("Nome_invalido");
            return;
        }
        jogador = partida.registrarJogador(nome, socket.getInetAddress(), this);

        if (jogador == null) {
            erroEntrada("Rodada_em_andamento");
            return;
        }
        enviar("ENTRADA;OK;" + jogador.getId());
        log("Entrou: " + jogador);
    }

    // Processa pedido de início da rodada
    private void tratarPedirInicio() {
        if (jogador == null) {
            log("PEDIR_INICIO ignorado: jogador ainda não entrou");
            return;
        }

        if (partida.totalJogadores() < 2) {
            log("Tentativa de iniciar bloqueada: " + jogador.getNome() + " está sozinho na sala.");
            return;
        }

        if (partida.iniciarRodada()) {
            log("Rodada iniciada por " + jogador);
        } else {
            log("PEDIR_INICIO ignorado de " + jogador + ": rodada já em andamento ou não há jogadores suficientes.");
        }
    }

    // Processa quando jogador bate STOP
    private void tratarBatiStop() {
        if (jogador == null) {
            log("BATI_STOP ignorado: jogador ainda não entrou");
            return;
        }
        if (partida.encerrarRodada()) {
            log("STOP aceito de " + jogador);
        } else {
            log("STOP ignorado de " + jogador);
        }
    }

    // Processa respostas enviadas pelo jogador
    private void tratarRespostas(String[] campos){
        if (jogador == null) {
            log("RESPOSTAS ignorado: jogador ainda não entrou");
            return;
        }

        int esperado = Protocolo.TEMAS.length + 1;
        if (campos.length != esperado) {
            log("RESPOSTAS ignorado de " + jogador + ": esperava " + esperado
                    + " campos, veio " + campos.length);
            return;
        }

        String[] palavras = Arrays.copyOfRange(campos, 1, campos.length);

        if (partida.registrarRespostas(jogador, palavras)) {
            log("Respostas recebidas de " + jogador);
        } else {
            log("RESPOSTAS ignorado de " + jogador + " (fora da coleta ou já respondeu)");
        }
    }

    // Processa pedido de saída do jogador
    private void tratarSair() {
        if (jogador == null) {
            log("SAIR ignorado: jogador ainda não entrou");
            return;
        }
        log("Jogador " + jogador.getNome() + " solicitou sair do jogo");
        limpar();
    }

    // Envia erro de entrada ao cliente
    private void erroEntrada(String motivo) {
        enviar("ENTRADA;ERRO;" + motivo);
    }

    // Log no console do servidor
    private void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }
}