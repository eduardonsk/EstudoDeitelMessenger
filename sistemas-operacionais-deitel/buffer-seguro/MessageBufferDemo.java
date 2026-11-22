import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MessageBufferDemo {
    private static final int PRODUCERS = 3;
    private static final int MESSAGES_PER_PRODUCER = 5;
    private static final int TOTAL = PRODUCERS * MESSAGES_PER_PRODUCER;

    public static void main(String[] args) throws Exception {
        MessageBuffer buffer = new MessageBuffer(2);
        ExecutorService workers = Executors.newFixedThreadPool(PRODUCERS + 1);
        try {
            Future<List<String>> consumer = workers.submit(() -> consume(buffer));
            List<Future<?>> producers = new ArrayList<>();
            Set<String> expected = new HashSet<>();
            for (int id = 1; id <= PRODUCERS; id++) {
                List<String> messages = new ArrayList<>();
                for (int number = 1; number <= MESSAGES_PER_PRODUCER; number++) {
                    messages.add("P" + id + "-M" + number);
                }
                expected.addAll(messages);
                producers.add(workers.submit(() -> {
                    for (String message : messages) {
                        buffer.put(message);
                        System.out.println("PRODUZIU " + message);
                    }
                    return null;
                }));
            }
            for (Future<?> producer : producers) {
                producer.get();
            }
            verify(consumer.get(), expected, buffer.size());
        } finally {
            workers.shutdownNow();
        }
    }

    private static List<String> consume(MessageBuffer buffer) throws InterruptedException {
        List<String> received = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) {
            String message = buffer.take();
            received.add(message);
            System.out.println("CONSUMIU " + message);
            Thread.sleep(30);
        }
        return received;
    }

    private static void verify(List<String> received, Set<String> expected, int remaining) {
        Set<String> unique = new HashSet<>(received);
        boolean ok = received.size() == TOTAL && unique.size() == TOTAL
                && unique.equals(expected) && remaining == 0;
        System.out.printf("RESULTADO: %s | recebidas=%d | unicas=%d | restantes=%d%n",
                ok ? "SUCESSO" : "FALHA", received.size(), unique.size(), remaining);
        if (!ok) {
            throw new AssertionError("Perda, duplicacao ou valor inesperado");
        }
    }
}
