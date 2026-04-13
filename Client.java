import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Client {
    private static final int CONCURRENT_REQUESTS = 100;
    private static final String TARGET_HOST = "localhost";
    private static final int TARGET_PORT = 8010;

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(CONCURRENT_REQUESTS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalLatencyMs = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        System.out.println("Preparing " + CONCURRENT_REQUESTS + " concurrent workers...");

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            executor.execute(() -> {
                try {
                    startSignal.await(); 

                    long start = System.nanoTime();
                    try (Socket socket = new Socket(TARGET_HOST, TARGET_PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                        
                        out.println("Stress test packet from thread: " + Thread.currentThread().threadId());
                        in.readLine();
                        
                        long end = System.nanoTime();
                        totalLatencyMs.addAndGet((end - start) / 1_000_000);
                        successCount.incrementAndGet();
                        
                    } catch (IOException e) {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishSignal.countDown();
                }
            });
        }

        long testStartTime = System.currentTimeMillis();
        System.out.println("Firing requests now!");
        startSignal.countDown(); 

        finishSignal.await(); 
        long testEndTime = System.currentTimeMillis();

        report(successCount.get(), failureCount.get(), totalLatencyMs.get(), testEndTime - testStartTime);
        executor.shutdown();
    }

    private static void report(int ok, int fail, long lat, long time) {
        System.out.println("\n--- Performance Report ---");
        System.out.println("Total Requests:   " + (ok + fail));
        System.out.println("Successful:       " + ok);
        System.out.println("Failed:           " + fail);
        if (ok > 0) {
            System.out.println("Average Latency:  " + (lat / ok) + " ms");
        }
        System.out.println("Execution Time:   " + time + " ms");
        double rps = (double) ok / (time / 1000.0);
        System.out.println("Throughput:       " + String.format("%.2f", rps) + " requests/sec");
        System.out.println("--------------------------");
    }
}
