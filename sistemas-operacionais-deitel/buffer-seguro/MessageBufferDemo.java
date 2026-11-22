import java.util.*;
import java.util.concurrent.*;

public class MessageBufferDemo {
    public static void main(String[] args) throws Exception {
        final int producers = 3, perProducer = 5, total = producers * perProducer;
        MessageBuffer buffer = new MessageBuffer(2);
        ExecutorService pool = Executors.newFixedThreadPool(producers + 1);
        List<String> received = Collections.synchronizedList(new ArrayList<>());
        Future<?> consumer = pool.submit(() -> {
            try {
                for (int i = 0; i < total; i++) {
                    String msg = buffer.take();
                    received.add(msg);
                    System.out.println("CONSUMIU " + msg);
                    Thread.sleep(30);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        List<Future<?>> tasks = new ArrayList<>();
        for (int id = 1; id <= producers; id++) {
            final int producerId = id;
            tasks.add(pool.submit(() -> {
                try {
                    for (int n = 1; n <= perProducer; n++) {
                        String msg = "P" + producerId + "-M" + n;
                        buffer.put(msg);
                        System.out.println("PRODUZIU " + msg);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));
        }
        for (Future<?> task : tasks) task.get();
        consumer.get();
        pool.shutdown();
        Set<String> expected = new HashSet<>();
        for (int i=1;i<=producers;i++) for (int j=1;j<=perProducer;j++) expected.add("P"+i+"-M"+j);
        boolean ok = received.size() == total && new HashSet<>(received).equals(expected) && buffer.size() == 0;
        System.out.println("RESULTADO: " + (ok ? "SUCESSO" : "FALHA") + " | recebidas=" + received.size() + " | unicas=" + new HashSet<>(received).size() + " | restantes=" + buffer.size());
        if (!ok) throw new AssertionError("Perda, duplicacao ou valor inesperado");
    }
}
