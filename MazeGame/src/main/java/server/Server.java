package server;

import game.PlayerMove;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
// Some basic setups for the server, will need to implement a lot of the server functions
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private static final int PORT = 42042; // Random number, can be changed if needed
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final BlockingQueue<PlayerMove> moves = new LinkedBlockingQueue<>(); // Global queue used for
                                                                                        // andling
    // player
    // moves. Each client thread validate moves
    // then queues to this queue.
    private static int nextPlayerId = 1; // Starts at 1 by default, we can randomize it but I don't think it's necessary

    public static void main(String args[]) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));) {
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

    // Handles a new client connection
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader inReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream()) {
            // Read and validate auth string
            char[] authBuffer = new char[VALID_AUTH.length()];
            inReader.read(authBuffer, 0, VALID_AUTH.length());

            // Check length first in case somebody spamming or something, idk
            // TODO: double check for valid socket closing andclosing other things
            if (!isValidAuth(authBuffer)) {
                System.out.println(clientSocket.getInetAddress() + " Client rejected, auth: " + new String(authBuffer));
                clientSocket.close();
                return;
            }

            // Assign user ID and send to the client
            int playerId = getNextPlayerId();

            System.out.println("Assigned user Id: " + playerId);
            out.write(playerId);

            // Add to clients list
            ClientHandler clientHandler = new ClientHandler(playerId, clientSocket, in, out);
            clients.put(playerId, clientHandler);

            clientHandler.handleMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper function used only in handleClient()
    private static boolean isValidAuth(char[] auth) {
        for (int i = 0; i < VALID_AUTH.length(); i++) {
            if (auth[i] != VALID_AUTH.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static synchronized int getNextPlayerId() {
        int temp = nextPlayerId;

        // In the unlikely case of overflow, wrap it back
        if (temp == Byte.MAX_VALUE) {
            nextPlayerId = 1;
        } else {
            nextPlayerId++;
        }

        return temp;
    }

    static class ClientHandler {
        private final int playerId;
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;

        public ClientHandler(int playerId, Socket socket, InputStream in, OutputStream out) {
            this.playerId = playerId;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        public void broadCastMove(int playerId, int row, int col) {
            try {
                broadcast(playerId, row + "," + col);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void handleMessages() throws IOException {
            // placeholder
        }
    }

    // TODO: change this
    private static void broadcast(int sourcePlayerId, String message) throws IOException {
        for (ClientHandler client : clients.values()) {
            // Do not send to the user who sent the original message
            if (client.playerId != sourcePlayerId) {
                client.out.write(message.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
