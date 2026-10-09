package client;

import common.Mensagem;
import common.Protocolo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class OuvinteTCP implements Runnable {

    private final BufferedReader in;
    private final PrintWriter out;
    private final EstadoCliente estado;
    private final String nomeJogador;
    private volatile boolean rodando = true; // Flag para parar a thread

    public OuvinteTCP(BufferedReader in, PrintWriter out, EstadoCliente estado, String nomeJogador) {
        this.in = in;
        this.out = out;
        this.estado = estado;
        this.nomeJogador = nomeJogador;
    }

    // Sinaliza para a thread parar
    public void parar() {
        this.rodando = false;
    }

    @Override
    public void run() {
        try {
            String linha;
            // Loop principal: escuta mensagens do servidor
            while (rodando && (linha = in.readLine()) != null) {
                // Mensagem de sistema (jogador saiu)
                if (linha.startsWith("SISTEMA:O jogador ")) {
                    System.out.println("\n[SISTEMA] " + linha.substring(8).trim());
                    System.out.println("Pode aguardar por um novo jogador ou digitar 'sair' para fechar também.");
                    System.out.print("> ");
                    continue;
                }
                Mensagem msg = Mensagem.parse(linha);

                switch (msg.tipo) {
                    case INICIO_RODADA:
                        estado.limparRodada();
                        estado.setEstado(EstadoCliente.Estado.EM_RODADA);
                        if (msg.campos.length > 0 && !msg.campos[0].isEmpty()) {
                            estado.setLetra(msg.campos[0].charAt(0));
                        }

                        System.out.println("\n======================================");
                        System.out.println("         A RODADA COMEÇOU!!           ");
                        System.out.println("======================================");
                        System.out.println("LETRA SORTEADA: " + estado.getLetra());
                        System.out.println("--------------------------------------");
                        System.out.print("> " + Protocolo.TEMAS[0] + ": ");
                        break;

                    case PAREM_TUDO:
                        // Alguém bateu STOP ou tempo esgotou
                        if (estado.getEstado() == EstadoCliente.Estado.EM_RODADA) {
                            estado.setEstado(EstadoCliente.Estado.AGUARDANDO_RESULTADO);
                            System.out.println("\n\n[ALERTA] STOP BATIDO! Tempo esgotado.");
                            System.out.println("(Pressione ENTER para continuar...)");

                            String[] palavras = estado.getPalavras();
                            String[] camposResposta = new String[1 + palavras.length];
                            camposResposta[0] = nomeJogador;

                            for (int i = 0; i < palavras.length; i++) {
                                camposResposta[i + 1] = (palavras[i] != null) ? palavras[i] : "";
                            }

                            Mensagem resposta = new Mensagem(Protocolo.Tipo.RESPOSTAS, camposResposta);
                            out.println(resposta.paraLinha());
                        }
                        break;

                    case RESULTADO:
                        estado.setEstado(EstadoCliente.Estado.LOBBY);
                        System.out.println("\n======================================");
                        System.out.println("          RESULTADO DA RODADA         ");
                        System.out.println("======================================");
                        for (String placar : msg.campos) {
                            System.out.println(" -> " + placar);
                        }
                        System.out.println("======================================");
                        System.out.println("Digite 'iniciar' para uma nova rodada ou 'sair' para fechar.");
                        System.out.print("> ");
                        break;

                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("\n[SISTEMA] Conexão com o servidor foi encerrada.");
            System.exit(0);
        }
    }
}
