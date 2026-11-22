import com.deitel.messenger.MessageListener;
import com.deitel.messenger.sockets.client.SocketMessageManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClienteDeTeste {
    public static void main(String[] args) {
        if (args.length != 2 || !("Alice".equals(args[0]) || "Bruno".equals(args[0]))) {
            System.err.println("Uso: ClienteDeTeste <Alice|Bruno> <identificador-do-ensaio>");
            System.exit(1);
        }

        String nome = args[0];
        String textoEsperado = "Ola, Bruno! Ensaio " + args[1];
        CountDownLatch mensagemRecebida = new CountDownLatch(1);
        SocketMessageManager cliente = new SocketMessageManager("127.0.0.1");
        MessageListener leitor = (remetente, mensagem) -> {
            if ("Alice".equals(remetente) && textoEsperado.equals(mensagem)) {
                System.out.printf("%s recebeu em MessageListener: %s -> %s%n",
                    nome, remetente, mensagem);
                mensagemRecebida.countDown();
            }
        };

        int codigoDeSaida = 1;
        try {
            cliente.connect(leitor);
            System.out.println(nome + " iniciou a escuta");
            if ("Alice".equals(nome)) {
                System.out.println("Alice chamou sendMessage: " + textoEsperado);
                cliente.sendMessage(nome, textoEsperado);
            }
            if (!mensagemRecebida.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo de recepcao esgotado para " + nome);
            }
            codigoDeSaida = 0;
        } catch (Exception erro) {
            erro.printStackTrace();
        } finally {
            cliente.disconnect(leitor);
        }
        System.exit(codigoDeSaida);
    }
}