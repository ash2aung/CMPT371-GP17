package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
// Some basic setups for the server, will need to implement a lot of the server functions

public class Server {
    private static final int PORT = 42042; // Random number, can be changed if needed
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static int nextPlayerId = 1; // Starts at 1 by default, we can randomize it but I don't think it's necessary

    public static void main(String args[]) throws IOException {
        InetAddress bindAddr = InetAddress.getByName("0.0.0.0");
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, bindAddr);) {
            System.out.println("Server started on port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Incoming connection attempt from " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            // Read and validate auth string
            String auth = in.readLine();
            // Check length first in case somebody spamming or something, idk
            if (auth.length() != VALID_AUTH.length() || !auth.equals(VALID_AUTH)) {
                System.out.println(clientSocket.getInetAddress() + " Client rejected, auth: " + auth);
                clientSocket.close();
                return;
            }

            // Assign user ID and send to the client
            int playerId = getNextPlayerId();

            System.out.println("Assigned user Id: " + playerId);
            out.println(playerId);

            // Add to clients list
            ClientHandler clientHandler = new ClientHandler(playerId, clientSocket, in, out);
            clients.put(playerId, clientHandler);

            clientHandler.handleMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized int getNextPlayerId() {
        int temp = nextPlayerId;

        // In the unlikely case of overflow, wrap it back
        if (temp == Integer.MAX_VALUE) {
            nextPlayerId = 1;
        } else {
            nextPlayerId++;
        }

        return temp;
    }

    static class ClientHandler {
        private final int playerId;
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        public ClientHandler(int playerId, Socket socket, BufferedReader in, PrintWriter out) {
            this.playerId = playerId;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        public void broadCastMove(int playerId, int row, int col) {
            broadcast(playerId, row + "," + col);
        }

        public void handleMessages() throws IOException {
            // placeholder
        }
    }

    // Right now I think it is sending the playerId as a String, maybe we can look
    // into sending numbers directly instead of Strings
    private static void broadcast(int sourcePlayerId, String message) {
        for (ClientHandler client : clients.values()) {
            // Do not send to the user who sent the original message
            if (client.playerId != sourcePlayerId) {
                client.out.println("id:" + sourcePlayerId + "/" + message);
            }
        }
    }
}
