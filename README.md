# High-Performance Multithreaded TCP Server

A robust, concurrent TCP server and stress-testing client implemented purely in Java (JSR-166). This project demonstrates core systems programming concepts including multithreading, thread-pool management, socket I/O, and network synchronization without relying on external web frameworks.

## Architecture Overview
This project consists of two independent components communicating over the loopback interface:
1. **The Server:** A multithreaded TCP server utilizing an `ExecutorService` thread pool to handle concurrent requests with backpressure management.
2. **The Client:** A high-concurrency stress-testing tool that uses a `CountDownLatch` to simulate a "thundering herd" attack, measuring latency and throughput in real-time.

## Key Technical Features
- **Thread Pool Architecture:** Uses `ThreadPoolExecutor` with a fixed core size (tied to system CPU cores) and a `LinkedBlockingQueue` to prevent memory exhaustion (OOM).
- **Backpressure Handling:** Implements a `CallerRunsPolicy` to gracefully throttle incoming traffic when the server is at maximum capacity.
- **Synchronized Testing:** Employs a `CountDownLatch` to release 100+ threads simultaneously, ensuring a true test of the server's concurrency limits and queueing latency.
- **Graceful Shutdown:** Includes a JVM Two-Phase Shutdown Hook (`awaitTermination` -> `shutdownNow`) to ensure active connections are drained safely before process termination.
- **Resource Protection:** Uses Socket Timeouts (`setSoTimeout`) to drop stalled connections and prevent "Slowloris" style resource hogging.

## Performance Benchmarks
During local stress testing (100 concurrent workers), the architecture achieved:
- **Throughput:** ~680 Requests / Second
- **Average Queue Latency:** ~125 ms under heavy lock contention
- **Failure Rate:** 0%
- 
## Repository Structure

```text
java-multithreaded-server/
├── Client/
│   └── Client.java       # The stress-testing suite utilizing CountDownLatch
├── Server/
│   └── Server.java       # The multithreaded TCP server with ThreadPoolExecutor          
└── README.md             # Project documentation and performance metrics
