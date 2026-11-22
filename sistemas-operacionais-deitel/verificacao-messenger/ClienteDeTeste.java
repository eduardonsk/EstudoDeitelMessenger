import com.deitel.messenger.MessageListener;
import com.deitel.messenger.sockets.client.SocketMessageManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClienteDeTeste {
    public static void main(String[] args) {
        String nome = args[0];
        String texto = "Ola, Bruno! Ensaio " + args[1];
        CountDownLatch confirmado = new CountDownLatch(1);
        SocketMessageManager cliente = new SocketMessageManager("127.0.0.1");
        MessageListener leitor = (remetente, mensagem) -> {
            if ("Alice".equals(remetente) && texto.equals(mensagem)) {
                System.out.printf("%s recebeu em MessageListener: %s -> %s%n",
                    nome, remetente, mensagem);
                confirmado.countDown();
            }
        };

        int status = 1;
        try {
            cliente.connect(leitor);
            System.out.println(nome + " iniciou a escuta");
            if ("Alice".equals(nome)) {
                System.out.println("Alice chamou sendMessage: " + texto);
                cliente.sendMessage(nome, texto);
            }
            if (!confirmado.await(10, TimeUnit.SECONDS))
                throw new IllegalStateException("Tempo de recepcao esgotado para " + nome);
            status = 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cliente.disconnect(leitor);
        }
        // O executor do cliente do livro permanece ativo depois de disconnect.
        System.exit(status);
    }
}
