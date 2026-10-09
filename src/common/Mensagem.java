package common;

public class Mensagem {
    public final Protocolo.Tipo tipo;
    public final String[] campos;

    public Mensagem(Protocolo.Tipo tipo, String... campos) {
        this.tipo = tipo;
        this.campos = (campos != null) ? campos : new String[0];
    }

    // Converte string da rede em objeto Mensagem
    public static Mensagem parse(String linha) {
        if (linha == null || linha.trim().isEmpty()) {
            return new Mensagem(Protocolo.Tipo.INVALIDA);
        }

        // -1 preserva campos vazios no final
        String[] partes = linha.split(Protocolo.SEPARADOR, -1);

        try {
            Protocolo.Tipo tipo = Protocolo.Tipo.valueOf(partes[0].toUpperCase());
            String[] campos = new String[partes.length - 1];

            if (campos.length > 0) {
                System.arraycopy(partes, 1, campos, 0, campos.length);
            }

            return new Mensagem(tipo, campos);

        } catch (IllegalArgumentException e) {
            return new Mensagem(Protocolo.Tipo.INVALIDA);
        }
    }

    // Converte objeto Mensagem em string para envio
    public String paraLinha() {
        if (campos.length == 0) {
            return tipo.name();
        }
        return tipo.name() + Protocolo.SEPARADOR + String.join(Protocolo.SEPARADOR, campos);
    }
}