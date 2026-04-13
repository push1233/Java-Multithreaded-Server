import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    
    private static final int PORT = 8010;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final int CLIENT_TIMEOUT_MS = 5000;

    private final ExecutorService threadPool;
    private volatile boolean isRunning = true;

    public Server() {
        this.threadPool = new ThreadPoolExecutor(
            THREAD_POOL_SIZE, 
            THREAD_POOL_SIZE,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), 
            new ThreadPoolExecutor.CallerRunsPolicy() 
        );
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received. Closing server...");
            stop();
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setSoTimeout(1000); 
            logger.info("Server started on port " + PORT + " using " + THREAD_POOL_SIZE + " worker threads.");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(CLIENT_TIMEOUT_MS); 
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (SocketTimeoutException e) {
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Critical server socket error", e);
        }
    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();
            try (
                socket; 
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                String input = in.readLine();
                if (input != null) {
                    out.println("ACK: Processed " + input.length() + " bytes.");
                }
                
                long duration = (System.nanoTime() - startTime) / 1_000_000;
                logger.fine("Request handled in " + duration + "ms from " + socket.getRemoteSocketAddress());
                
            } catch (IOException e) {
                logger.warning("Connection error with client: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}

