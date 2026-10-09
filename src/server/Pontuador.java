package server;

import common.Protocolo;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calcula os pontos de uma rodada.
 * Regras: 10 pontos palavra única, 5 repetida, 0 vazia ou sem a letra sorteada.
 */
public class Pontuador {

    public static final int PONTOS_UNICA = 10;
    public static final int PONTOS_REPETIDA = 5;

    public static Map<Integer, Integer> pontuar(char letra, Map<Integer, String[]> respostas) {
        char letraNorm = Character.toLowerCase(letra);

        // Todos começam com 0 pontos
        Map<Integer, Integer> pontos = new LinkedHashMap<>();
        for (Integer id : respostas.keySet()) {
            pontos.put(id, 0);
        }

        // Cada tema é pontuado separadamente
        for (int i = 0; i < Protocolo.TEMAS.length; i++) {

            // Junta só as palavras válidas e conta quantas vezes cada uma apareceu
            Map<Integer, String> validas = new HashMap<>();
            Map<String, Integer> contagem = new HashMap<>();

            for (Map.Entry<Integer, String[]> e : respostas.entrySet()) {
                String palavra = normalizar(obter(e.getValue(), i));
                if (valida(palavra, letraNorm)) {
                    validas.put(e.getKey(), palavra);
                    contagem.merge(palavra, 1, Integer::sum);
                }
            }

            // Única (apareceu 1 vez) = 10, repetida (2 ou mais) = 5
            for (Map.Entry<Integer, String> e : validas.entrySet()) {
                int ganho = (contagem.get(e.getValue()) == 1) ? PONTOS_UNICA : PONTOS_REPETIDA;
                pontos.merge(e.getKey(), ganho, Integer::sum);
            }
        }

        return pontos;
    }

    // Pega a palavra da posição i, array nulo, curto ou posição nula viram ""
    private static String obter(String[] palavras, int i) {
        if (palavras == null || i >= palavras.length || palavras[i] == null) {
            return "";
        }
        return palavras[i];
    }

    // Tira espaços, passa para minúsculas e remove acentos
    private static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().replaceAll("\\s+", " ");
    }

    // Válida = não vazia e começa com a letra sorteada
    private static boolean valida(String palavraNormalizada, char letraNorm) {
        return !palavraNormalizada.isEmpty() && palavraNormalizada.charAt(0) == letraNorm;
    }
}