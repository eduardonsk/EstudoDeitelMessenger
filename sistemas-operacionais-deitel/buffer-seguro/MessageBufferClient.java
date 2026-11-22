import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MessageBufferClient {
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2 ||
                !(args[0].equals("A") || args[0].equals("B"))) {
            throw new IllegalArgumentException("Uso: java MessageBufferClient A|B [porta]");
        }
        String clientId = args[0];
        int port = args.length == 2 ? Integer.parseInt(args[1]) : 12345;
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(30_000);
            BufferedWriter output = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            for (int number = 1; number <= 3; number++) {
                String message = clientId + "-" + number;
                output.write(message);
                output.newLine();
                output.flush();
                System.out.println("ENVIOU " + message);
            }
            socket.shutdownOutput();

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String receipt = input.readLine();
            if (!"OK 3".equals(receipt)) {
                throw new IOException("Servidor não confirmou o recebimento das três mensagens");
            }
            System.out.println("CLIENTE " + clientId + " recebeu " + receipt);
        }
    }
}
