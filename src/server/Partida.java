package server;

import common.Protocolo;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Partida {

    // Estados da partida
    public enum Estado {
        AGUARDANDO,   // Aguardando jogadores
        RODADA,       // Rodada em andamento
        COLETANDO     // Coletando respostas após STOP
    }

    private static final String LETRAS = "ABCDEFGHIJLMNOPRSTUV";
    private static final long TEMPO_COLETA_MS = 10_000;

    private Estado estado = Estado.AGUARDANDO;
    private char letraAtual;
    private int numeroRodada = 0;
    private final Random random = new Random();

    private final AtomicInteger proximoId = new AtomicInteger(1);
    private final Map<Integer, Jogador> jogadores = new LinkedHashMap<>();

    // Respostas da rodada atual: id do jogador -> palavras
    private final Map<Integer, String[]> respostas = new HashMap<>();

    public synchronized Estado getEstado() {
        return estado;
    }

    // Registra novo jogador (só se não houver rodada em andamento)
    public synchronized Jogador registrarJogador(String nome, InetAddress ip, ClienteHandler handler) {
        if (estado != Estado.AGUARDANDO) {
            return null;   // rodada em andamento
        }
        int id = proximoId.getAndIncrement();
        Jogador jogador = new Jogador(id, nome, ip, handler);
        jogadores.put(id, jogador);
        return jogador;
    }

    // Remove jogador da partida
    public synchronized void removerJogador(int id) {
        Jogador jogadorRemovido = jogadores.remove(id);
        respostas.remove(id);   // respostas de quem saiu não contam

        // Avisa os demais se ainda houver jogadores
        if (jogadorRemovido != null && !jogadores.isEmpty()) {
            broadcast("SISTEMA: O jogador " + jogadorRemovido.getNome() + " saiu da partida.");
        }

        // Se ficou vazio, volta ao lobby
        if (jogadores.isEmpty()) {
            estado = Estado.AGUARDANDO;
            respostas.clear();
            return;
        }

        // Anti-travamento: se sobrou 1 jogador durante rodada, encerra
        if (estado == Estado.RODADA && jogadores.size() <= 1) {
            encerrarRodada();
        } else if (estado == Estado.COLETANDO && todosResponderam()) {
            finalizarRodada();
        }
    }

    public synchronized int totalJogadores() {
        return jogadores.size();
    }

    public synchronized List<Jogador> listarJogadores() {
        return new ArrayList<>(jogadores.values());
    }

    // ---------- Fluxo da rodada ----------

    // Inicia nova rodada (precisa de pelo menos 2 jogadores)
    public synchronized boolean iniciarRodada() {
        if (estado != Estado.AGUARDANDO || jogadores.size() < 2) {
            return false;
        }
        numeroRodada++;
        respostas.clear();
        letraAtual = LETRAS.charAt(random.nextInt(LETRAS.length()));
        estado = Estado.RODADA;
        broadcast("INICIO_RODADA" + Protocolo.SEPARADOR + letraAtual);
        return true;
    }

    // Encerra rodada quando alguém bate STOP
    public synchronized boolean encerrarRodada() {
        if (estado != Estado.RODADA) {
            return false;
        }
        estado = Estado.COLETANDO;
        broadcast("PAREM_TUDO");
        iniciarTimeoutColeta();
        return true;
    }

    // Registra respostas de um jogador
    public synchronized boolean registrarRespostas(Jogador jogador, String[] palavras) {
        if (estado != Estado.COLETANDO) {
            return false;
        }
        if (!jogadores.containsKey(jogador.getId())) {
            return false;
        }
        if (respostas.containsKey(jogador.getId())) {
            return false;   // já respondeu
        }

        respostas.put(jogador.getId(), palavras);

        if (todosResponderam()) {
            finalizarRodada();
        }
        return true;
    }

    // ---------- Fechamento e timeout ----------

    // Verifica se todos os jogadores responderam
    private boolean todosResponderam() {
        return respostas.keySet().containsAll(jogadores.keySet());
    }

    // Finaliza rodada e calcula pontuação
    private synchronized void finalizarRodada() {
        // Quem não respondeu entra com palavras vazias (0 pontos)
        Map<Integer, String[]> finais = new LinkedHashMap<>();
        for (Jogador j : jogadores.values()) {
            String[] r = respostas.get(j.getId());
            finais.put(j.getId(), r != null ? r : respostasVazias());
        }

        Map<Integer, Integer> pontos = Pontuador.pontuar(letraAtual, finais);

        // Envia resultados aos jogadores
        StringBuilder sb = new StringBuilder("RESULTADO");
        for (Jogador j : jogadores.values()) {
            sb.append(Protocolo.SEPARADOR).append(j).append(":").append(pontos.get(j.getId()));
        }
        broadcast(sb.toString());
        log("Rodada " + numeroRodada + " finalizada: " + sb);

        respostas.clear();
        estado = Estado.AGUARDANDO;
    }

    // Retorna array de strings vazias para jogador sem resposta
    private String[] respostasVazias() {
        String[] vazias = new String[Protocolo.TEMAS.length];
        Arrays.fill(vazias, "");
        return vazias;
    }

    // Inicia thread para timeout de coleta (fora do synchronized)
    private void iniciarTimeoutColeta() {
        final int rodadaDaColeta = numeroRodada;
        Thread despertador = new Thread(() -> {
            try {
                Thread.sleep(TEMPO_COLETA_MS);
            } catch (InterruptedException e) {
                return;
            }
            finalizarPorTempo(rodadaDaColeta);
        }, "timeout-rodada-" + rodadaDaColeta);
        despertador.setDaemon(true);
        despertador.start();
    }

    // Finaliza rodada por timeout (se ainda faz sentido)
    private synchronized void finalizarPorTempo(int rodada) {
        if (estado == Estado.COLETANDO && rodada == numeroRodada) {
            log("Timeout da coleta na rodada " + rodada);
            finalizarRodada();
        }
    }

    // Envia mensagem para todos os jogadores
    private void broadcast(String linha) {
        for (Jogador jogador : listarJogadores()) {
            jogador.getHandler().enviar(linha);
        }
    }

    private void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }
}