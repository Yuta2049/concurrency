package course.concurrency.m6_other;

import java.util.LinkedList;

public class BlockingQueue<T> {

    private final int maxSize;
    private final LinkedList<T> queue = new LinkedList<>();

    public BlockingQueue(int size) {
        maxSize = size;
    }

    public synchronized void enqueue(T value) {
        while (queue.size() == maxSize) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("InterruptedExceptions was thrown during enqueue");
            }
        }
        queue.add(value);
        notify();
    }

    public synchronized T dequeue() {
        while (queue.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("InterruptedExceptions was thrown during dequeue");
            }
        }
        T res = queue.removeFirst();
        notify();
        return res;
    }

    public synchronized int size() {
        return queue.size();
    }
}
