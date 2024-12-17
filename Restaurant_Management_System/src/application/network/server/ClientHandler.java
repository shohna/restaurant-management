package application.network.server;

import java.net.*;
import java.io.*;
import application.network.message.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RMSServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String clientId;

    public ClientHandler(Socket socket, RMSServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            setupStreams();
            handleClient();
        } catch (IOException e) {
            cleanUp();
        }
    }

    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    private void handleClient() {
        try {
            while (!socket.isClosed()) {
                NetworkMessage message = (NetworkMessage) in.readObject();
                NetworkMessage response = server.handleDatabaseOperation(message);
                sendResponse(response);
            }
        } catch (IOException | ClassNotFoundException e) {
            cleanUp();
        }
    }

    private void sendResponse(NetworkMessage response) {
        try {
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            cleanUp();
        }
    }

    private void cleanUp() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}