import java.util.ArrayDeque;
import java.util.Queue;

/** Monitor de mensagens FIFO, com capacidade limitada e bloqueio sem espera ativa. */
public final class MessageBuffer {
    private final Queue<String> queue = new ArrayDeque<>();
    private final int capacity;
    public MessageBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacidade deve ser positiva");
        this.capacity = capacity;
    }
    public synchronized void put(String message) throws InterruptedException {
        if (message == null) throw new IllegalArgumentException("mensagem nula");
        while (queue.size() == capacity) wait();
        queue.add(message);
        notifyAll();
    }
    public synchronized String take() throws InterruptedException {
        while (queue.isEmpty()) wait();
        String message = queue.remove();
        notifyAll();
        return message;
    }
    public synchronized int size() { return queue.size(); }
}
