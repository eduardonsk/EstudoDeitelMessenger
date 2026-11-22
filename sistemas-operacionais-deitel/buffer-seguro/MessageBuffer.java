import java.util.ArrayDeque;
import java.util.Queue;

public final class MessageBuffer {
    private final Queue<String> messages = new ArrayDeque<>();
    private final int capacity;

    public MessageBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacidade deve ser positiva");
        }
        this.capacity = capacity;
    }

    public synchronized void put(String message) throws InterruptedException {
        if (message == null) {
            throw new IllegalArgumentException("mensagem nula");
        }
        while (messages.size() == capacity) {
            wait();
        }
        messages.add(message);
        notifyAll();
    }

    public synchronized String take() throws InterruptedException {
        while (messages.isEmpty()) {
            wait();
        }
        String message = messages.remove();
        notifyAll();
        return message;
    }

    public synchronized int size() {
        return messages.size();
    }
}
