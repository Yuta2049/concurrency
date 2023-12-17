package course.concurrency.m6_other;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

        //Отправляем в очередь 11 элементов в другом потоке
        Thread addingThread = new Thread(() -> {
            for (int i = 0; i < 11; i++) queue.enqueue(i);
        });
        addingThread.start();
        Thread.sleep(50);

        // Проверяем, что добавилось только 10 элементов
        assertEquals(10, queue.size());

        // Проверяем, что добавляющий поток заблокировался
        assertEquals(Thread.State.WAITING, addingThread.getState());

        // Вынимаем первый элемент из очереди
        queue.dequeue();
        Thread.sleep(50);

        //Проверяем, что в очереди все равно 10 элементов
        assertEquals(10, queue.size());
        Thread.sleep(50);

        // Проверяем, что добавляющий поток завершился
        assertEquals(Thread.State.TERMINATED, addingThread.getState());
    }

    @Test
    @DisplayName("Queue returns elements in FIFO order")
    void elementsReturnsInFIFOOrder() {

        BlockingQueue<Integer> queue = new BlockingQueue<>(10);
        //Добавляем элементы
        new Thread(() -> {
            for (int i = 0; i < 20; i++) queue.enqueue(i);
        }).start();

        // Вынимаем элементы, проверяя порядок
        for (int i = 0; i < 20; i++) {
            assertEquals(i, queue.dequeue());
        }
    }

    @Test
    @DisplayName("Thread will be blocked if queue is empty")
    void ThreadWillBlockIfQueueIsEmpty() throws InterruptedException {

        BlockingQueue<Integer> queue = new BlockingQueue<>(10);
        Thread thread = new Thread(queue::dequeue);
        thread.start();

        Thread.sleep(50);
        assertEquals(Thread.State.WAITING, thread.getState());
    }

    @Test
    @DisplayName("Queue returns all elements")
    void canReturnAllElements() throws InterruptedException {
        int elemQuantity = 1_000_000;

        CountDownLatch latch = new CountDownLatch(1);
        BlockingQueue<Integer> queue = new BlockingQueue<>(1000);
        Map<Integer, Integer> resultMap = new ConcurrentHashMap<>();

        ExecutorService executorService1 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()/2);
        ExecutorService executorService2 = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()/2);

        // Добавляем элементы
        for (int i = 0; i < elemQuantity; i++) {
            int finalI = i;
            executorService1.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ex) {/* do nothing */}
                queue.enqueue(finalI);
            });
            executorService2.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ex) {/* do nothing */}
                resultMap.put(queue.dequeue(), 1);
            });
        }

        latch.countDown();

        // Ждем, пока потоки отбегут
        executorService1.awaitTermination(500, TimeUnit.MILLISECONDS);
        executorService2.awaitTermination(500, TimeUnit.MILLISECONDS);

        // Проверяем, что элементы не потерялись
        assertEquals(elemQuantity, resultMap.size());
        List<Integer> resultList = new ArrayList<>(resultMap.keySet()).stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < elemQuantity; i++) {
            assertEquals(i, resultList.get(i));
        }
    }
}
