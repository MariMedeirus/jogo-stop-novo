package server;

import java.net.InetAddress;

public class Jogador {
    private final int Id;
    private final String Nome;
    private final InetAddress Ip;
    private final ClienteHandler Handler;

    public Jogador(int id, String nome, InetAddress ip, ClienteHandler handler) {
        this.Id = id;
        this.Nome = nome;
        this.Ip = ip;
        this.Handler = handler;
    }

    public int getId() {
        return Id;
    }

    public String getNome() {
        return Nome;
    }

    public InetAddress getIp() {
        return Ip;
    }

    public ClienteHandler getHandler() {
        return Handler;
    }

    @Override
    public String toString() {
        return Nome + "#" + Id;
    }
}
