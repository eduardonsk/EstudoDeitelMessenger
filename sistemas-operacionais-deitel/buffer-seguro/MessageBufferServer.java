import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Demonstração de dois clientes TCP alimentando o mesmo monitor no servidor. */
public class MessageBufferServer {
    private static final int CLIENT_COUNT = 2;
    private static final int MESSAGES_PER_CLIENT = 3;
    private static final int CAPACITY = 2;

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 12345 : Integer.parseInt(args[0]);
        MessageBuffer buffer = new MessageBuffer(CAPACITY);
        try (ServerSocket server = new ServerSocket(port, CLIENT_COUNT, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(120_000);
            System.out.printf("SERVIDOR porta=%d capacidade=%d%n", server.getLocalPort(), CAPACITY);
            ExecutorService workers = Executors.newFixedThreadPool(CLIENT_COUNT + 1);
            try {
                Future<List<String>> consumer = workers.submit(() -> consume(buffer));
                List<Future<Void>> producers = new ArrayList<>();
                for (int i = 0; i < CLIENT_COUNT; i++) {
                    Socket client = server.accept();
                    producers.add(workers.submit(() -> {
                        receive(client, buffer);
                        return null;
                    }));
                }
                for (Future<Void> producer : producers) {
                    producer.get();
                }
                verify(consumer.get(), buffer.size());
            } finally {
                workers.shutdownNow();
            }
        }
    }

    private static void receive(Socket client, MessageBuffer buffer) throws IOException, InterruptedException {
        try (Socket connection = client;
             BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
            connection.setSoTimeout(30_000);
            for (int n = 0; n < MESSAGES_PER_CLIENT; n++) {
                String message = in.readLine();
                if (message == null) {
                    throw new EOFException("Cliente encerrou antes de enviar três mensagens");
                }
                buffer.put(message);
            }
            if (in.readLine() != null) {
                throw new IOException("Cliente enviou mais de três mensagens");
            }
            out.write("OK 3\n");
            out.flush();
        }
    }

    private static List<String> consume(MessageBuffer buffer) throws InterruptedException {
        List<String> received = new ArrayList<>();
        for (int n = 0; n < CLIENT_COUNT * MESSAGES_PER_CLIENT; n++) {
            String message = buffer.take();
            received.add(message);
            System.out.println("CONSUMIU " + message);
        }
        return received;
    }

    private static void verify(List<String> received, int remaining) {
        Set<String> expected = new HashSet<>(Arrays.asList("A-1", "A-2", "A-3", "B-1", "B-2", "B-3"));
        Set<String> unique = new HashSet<>(received);
        boolean ok = received.size() == expected.size() && unique.equals(expected) && remaining == 0;
        System.out.printf("RESULTADO: %s | recebidas=%d | unicas=%d | restantes=%d%n",
                ok ? "SUCESSO" : "FALHA", received.size(), unique.size(), remaining);
        if (!ok) {
            throw new IllegalStateException("Mensagens perdidas, repetidas ou inesperadas");
        }
    }
}
