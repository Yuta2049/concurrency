import java.util.concurrent.*;

public class Executorss {
    public static long longTask() throws InterruptedException {
        Thread.sleep(1000); // + try-catch
        return ThreadLocalRandom.current().nextInt();
    }

    public static void main(String[] args) {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        //ExecutorService executor = Executors.newFixedThreadPool(8);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> longTask());
            System.out.print(executor.getPoolSize() + " ");
        }

        executor.shutdown();
    }
}
