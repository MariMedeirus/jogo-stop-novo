package client;

import common.Mensagem;
import common.Protocolo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);

        System.out.println("=== BEM-VINDO AO STOP!! ===");

        // Tenta descobrir servidor automaticamente
        String ip = DescobertaCliente.buscarServidor();

        // Se não encontrar, pede IP manualmente
        if (ip == null) {
            System.out.println("Iniciando modo manual de conexão...");
            System.out.print("Introduza o IP do servidor (deixe em branco para localhost): ");
            ip = teclado.nextLine().trim();
            if (ip.isEmpty()) {
                ip = "127.0.0.1";
            }
        }

        System.out.print("Seu nome: ");
        String nome = teclado.nextLine().trim();

        // Conecta ao servidor via TCP
        try (Socket socket = new Socket(ip, Protocolo.PORTA_TCP);
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Envia pedido de entrada
            Mensagem msgEntrar = new Mensagem(Protocolo.Tipo.ENTRAR, nome);
            out.println(msgEntrar.paraLinha());
            System.out.println("> A enviar pedido de conexão...");

            String respostaRaw = in.readLine();

            // Verifica se servidor respondeu
            if (respostaRaw == null) {
                System.out.println("[ERRO] O servidor fechou a conexão sem responder.");
                return;
            }

            Mensagem resposta = Mensagem.parse(respostaRaw);

            // Verifica se entrada foi aceita
            if (resposta.tipo == Protocolo.Tipo.ENTRADA && resposta.campos.length > 1 && resposta.campos[0].equalsIgnoreCase("OK")) {
                System.out.println("[SUCESSO] Entrada confirmada! O seu ID é: " + resposta.campos[1]);

                // Inicia thread para escutar mensagens do servidor
                EstadoCliente estado = new EstadoCliente();
                OuvinteTCP ouvinte = new OuvinteTCP(in, out, estado, nome);
                Thread threadOuvinte = new Thread(ouvinte);
                threadOuvinte.start();

                System.out.println("Digite 'iniciar' a qualquer momento para começar a rodada ou 'sair' para fechar.");
                System.out.print("> ");

                // Loop principal: processa comandos do usuário
                while (teclado.hasNextLine()) {
                    String input = teclado.nextLine().trim();
                    EstadoCliente.Estado estadoAtual = estado.getEstado();

                    if (estadoAtual == EstadoCliente.Estado.LOBBY) {
                        // Comandos disponíveis no lobby
                        if (input.equalsIgnoreCase("iniciar")) {
                            Mensagem msgInicio = new Mensagem(Protocolo.Tipo.PEDIR_INICIO);
                            out.println(msgInicio.paraLinha());
                            System.out.println("> Pedido de início enviado. A aguardar servidor...");

                        } else if (input.equalsIgnoreCase("sair")) {
                            System.out.println("Encerrando jogo.");
                            ouvinte.parar(); // Para thread de escuta
                            Mensagem msgSair = new Mensagem(Protocolo.Tipo.SAIR);
                            out.println(msgSair.paraLinha());
                            try {
                                threadOuvinte.join(1000); // Aguarda thread terminar
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            break;

                        } else if (!input.isEmpty()) {
                            System.out.println("Comando inválido no Lobby. Digite 'iniciar' ou 'sair'.");
                            System.out.print("> ");
                        }
                    } else if (estadoAtual == EstadoCliente.Estado.EM_RODADA) {
                        // Jogador preenchendo palavras
                        int idx = estado.getIndicePreenchimento();

                        if (idx < Protocolo.TEMAS.length) {
                            estado.setPalavra(idx, input);
                            estado.avancarIndice();

                            int proximoIdx = estado.getIndicePreenchimento();
                            if (proximoIdx < Protocolo.TEMAS.length) {
                                System.out.print("> " + Protocolo.TEMAS[proximoIdx] + ": ");
                            } else {
                                System.out.print("\n[!] Concluído! Digite 'STOP' para bater: ");
                            }
                        } else {
                            // Jogador bateu STOP
                            if (input.equalsIgnoreCase("STOP")) {
                                estado.setEstado(EstadoCliente.Estado.AGUARDANDO_RESULTADO);

                                // Envia aviso de STOP
                                Mensagem msgStop = new Mensagem(Protocolo.Tipo.BATI_STOP);
                                out.println(msgStop.paraLinha());

                                // Envia as respostas
                                String[] palavras = estado.getPalavras();
                                String[] campos = new String[1 + palavras.length];
                                campos[0] = nome;
                                for (int i = 0; i < palavras.length; i++) {
                                    campos[i + 1] = (palavras[i] != null) ? palavras[i] : "";
                                }
                                Mensagem msgRespostas = new Mensagem(Protocolo.Tipo.RESPOSTAS, campos);
                                out.println(msgRespostas.paraLinha());

                                System.out.println("> BATEU STOP! Respostas submetidas...");
                            }
                        }
                    }
                }

            } else {
                // Entrada recusada pelo servidor
                String motivo = resposta.campos.length > 1 ? resposta.campos[1] : "desconhecido";
                System.out.println("[ERRO] Entrada recusada pelo servidor: " + motivo);
            }

        } catch (IOException e) {
            System.out.println("[ERRO DE LIGAÇÃO] Não foi possível ligar ao servidor em " + ip);
            System.out.println("Detalhe: " + e.getMessage());
        } finally {
            teclado.close();
        }
    }
}