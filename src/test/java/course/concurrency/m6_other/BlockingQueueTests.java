package course.concurrency.m6_other;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class BlockingQueueTests {

    @Test
    @DisplayName("Queue can enqueue and dequeue elements")
    void canEnqueueAndDequeueElements() {
        BlockingQueue<Integer> queue = new BlockingQueue<>(10);
        for (int i = 0; i < 10; i++) {
            queue.enqueue(i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(i, queue.dequeue());
        }
    }

    @Test
    @DisplayName("Queue doesn't enqueue elements after max size")
    void canEnqueueAndBlockedOnMaxSize() throws InterruptedException {
        BlockingQueue<Integer> queue = new BlockingQueue<>(10);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        //Отправляем в очередь 11 элементов в другом потоке
        executorService.submit(() -> {
            for (int i = 0; i < 11; i++) {
                queue.enqueue(i);
            }
        });
        Thread.sleep(100);

        // Проверяем, что добавилось только 10 элементов
        assertEquals(10, queue.size());

        // Вынимаем первый
        assertEquals(0, queue.dequeue());
        Thread.sleep(100);

        //Проверяем, что в очереди все равно 10 элементов
        assertEquals(10, queue.size());

        for (int i = 1; i < 10; i++) {
            assertEquals(i, queue.dequeue());
        }
    }

    @Test
    @DisplayName("Queue returns elements in FIFO order")
    void elementsReturnsInFIFOOrder() throws InterruptedException {

        List<Integer> list = new ArrayList<>();
        BlockingQueue<Integer> queue = new BlockingQueue<>(10);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // Отправляем 20 элементов в другом потоке
        executorService.submit(() -> {
            for (int i = 0; i < 20; i++) {
                queue.enqueue(i);
            }
        });

        // Проверяем, что добавилось только 10 элементов
        Thread.sleep(100);
        assertEquals(10, queue.size());

        // Вынимаем элементы, проверяем порядок
        for (int i = 0; i < 20; i++) {
            assertEquals(i, queue.dequeue());
        }
    }

    @Test
    @DisplayName("Queue returns all elements")
    void canReturnAllElements() throws InterruptedException {
        int elemQuantity = 1_000_000;
        BlockingQueue<Integer> queue = new BlockingQueue<>(1000);

        Map<Integer, Integer> sourceMap = new ConcurrentHashMap<>();
        Map<Integer, Integer> resultMap = new ConcurrentHashMap<>();

        ExecutorService executorService1 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
        ExecutorService executorService2 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);

        // Добавляем элементы
        for (int i = 0; i < elemQuantity; i++) {
            sourceMap.put(i, 1);
            int finalI = i;
            executorService1.submit(() -> queue.enqueue(finalI));
        }

        for (int i = 0; i < elemQuantity; i++) {
            sourceMap.put(i, 1);
            executorService2.submit(() -> resultMap.put(queue.dequeue(), 1));
        }

        Thread.sleep(1000);
        assertEquals(sourceMap.keySet(), resultMap.keySet());
    }
}
