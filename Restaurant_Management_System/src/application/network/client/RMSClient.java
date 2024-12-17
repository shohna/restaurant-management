package application.network.client;

import application.network.message.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class RMSClient {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final ExecutorService executorService;
    private final BlockingQueue<NetworkMessage> responseQueue;
    private volatile boolean isRunning = true;

    public RMSClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.responseQueue = new LinkedBlockingQueue<>();  // Initialize the queue
        
        // Create streams in the correct order and flush immediately
        this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.out.flush();
        this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        this.executorService = Executors.newFixedThreadPool(2);
        
        System.out.println("Connected to server at " + host + ":" + port);
        startResponseHandler();  // Start listening for responses
    }

    private void startResponseHandler() {
        executorService.submit(() -> {
            while (isRunning) {
                try {
                    NetworkMessage response = (NetworkMessage) in.readObject();
                    responseQueue.put(response);
//                    System.out.println("Response queued: " + response);
                    if (response.getType() != MessageType.ERROR) {
                        System.out.println("Response queued: " + response);
                    }
                } catch (Exception e) {
                    if (isRunning) {
                        System.err.println("Error in response handler: " + e.getMessage());
                        handleDisconnection();
                    }
                    break;
                }
            }
        });
    }

    public CompletableFuture<NetworkMessage> sendMessage(MessageType type, Object payload) {
        CompletableFuture<NetworkMessage> future = new CompletableFuture<>();
        
        executorService.submit(() -> {
            try {
                // Create and send the message
                NetworkMessage message = new NetworkMessage(type, payload);
                System.out.println("Sending message: " + message);
                synchronized (out) {
                    out.writeObject(message);
                    out.flush();
                    out.reset(); // Reset the stream cache
                }

                // Wait for response
                NetworkMessage response = responseQueue.poll(10, TimeUnit.SECONDS);
                if (response != null) {
                    System.out.println("Received response: " + response);
                    future.complete(response);
                } else {
                    throw new TimeoutException("No response received within timeout");
                }
            } catch (Exception e) {
                System.err.println("Error in sendMessage: " + e.getMessage());
                future.completeExceptionally(e);
                handleDisconnection();
            }
        });

        return future;
    }

    private void handleDisconnection() {
        if (isRunning) {
            System.out.println("Connection lost. Cleaning up resources...");
            close();
        }
    }

    public void close() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) in.close();
            if (socket != null) socket.close();
            System.out.println("Client cleanup completed");
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}