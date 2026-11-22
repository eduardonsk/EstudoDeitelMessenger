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

public class MessageBufferServer {
    private static final int CLIENTS = 2;
    private static final int MESSAGES_PER_CLIENT = 3;
    private static final int CAPACITY = 2;

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 12345 : Integer.parseInt(args[0]);
        MessageBuffer buffer = new MessageBuffer(CAPACITY);
        ExecutorService workers = Executors.newFixedThreadPool(CLIENTS + 1);
        try (ServerSocket server = new ServerSocket(port, CLIENTS, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(120_000);
            System.out.printf("SERVIDOR porta=%d capacidade=%d%n", server.getLocalPort(), CAPACITY);
            Future<List<String>> consumer = workers.submit(() -> consume(buffer));
            List<Future<?>> connections = new ArrayList<>();
            for (int i = 0; i < CLIENTS; i++) {
                Socket client = server.accept();
                connections.add(workers.submit(() -> {
                    receive(client, buffer);
                    return null;
                }));
            }
            for (Future<?> connection : connections) {
                connection.get();
            }
            verify(consumer.get(), buffer.size());
        } finally {
            workers.shutdownNow();
        }
    }

    private static void receive(Socket client, MessageBuffer buffer) throws IOException, InterruptedException {
        try (Socket socket = client) {
            socket.setSoTimeout(30_000);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            for (int i = 0; i < MESSAGES_PER_CLIENT; i++) {
                String message = input.readLine();
                if (message == null) {
                    throw new EOFException("Cliente encerrou antes de enviar três mensagens");
                }
                buffer.put(message);
            }
            if (input.readLine() != null) {
                throw new IOException("Cliente enviou mais de três mensagens");
            }
            BufferedWriter output = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            output.write("OK 3\n");
            output.flush();
        }
    }

    private static List<String> consume(MessageBuffer buffer) throws InterruptedException {
        List<String> received = new ArrayList<>();
        for (int i = 0; i < CLIENTS * MESSAGES_PER_CLIENT; i++) {
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
