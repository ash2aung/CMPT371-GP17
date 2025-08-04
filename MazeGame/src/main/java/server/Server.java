package server;

import game.PlayerMove;
import game.Maze;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
// Some basic setups for the server, will need to implement a lot of the server functions
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private static final int PORT = 42042; // Random number, can be changed if needed
    private static ServerSocket serverSocket;
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final BlockingQueue<PlayerMove> moves = new LinkedBlockingQueue<>();
    // Global queue used for handling player moves. Each client thread validate
    // moves then queues to this queue.
    private static int nextPlayerId; // Starts at 1 by default, we can randomize it but I don't think it's necessary
    private static int[] cheeseCoords = new int[2];
    private static final int[] PIDS = { 0, 1, 2, 3 }; // Player ids
    private static List<Integer> availablePlayerIds;
    private static Maze maze;

    public static void main(String args[]) throws IOException {
        try {
            serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));
            System.out.println("Server started on port: " + PORT);

            while (true) {
                launchMatch();
                // Once a match ends, relaunch match with reset state
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void launchMatch() {
        matchInit();
        while (!availablePlayerIds.isEmpty()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Incoming connection attempt from " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("4 players connected.");
        // Start the game
    }

    // Sets states to starting defaults
    private static void matchInit() {
        // Create new random maze
        maze = new Maze();
        // Reset available player Ids
        availablePlayerIds = Arrays.stream(PIDS).boxed().toList();
        // Cheese removed
        cheeseCoords[0] = -1;
        cheeseCoords[1] = -1;
        // Reset player id
        nextPlayerId = 0;
    }

    // Handles a new client connection
    private static void handleClient(Socket clientSocket) {
        int playerId = -1;
        try (BufferedReader inReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream()) {
            // Read and validate auth string
            char[] authBuffer = new char[VALID_AUTH.length()];
            inReader.read(authBuffer, 0, VALID_AUTH.length());

            // Check length first in case somebody spamming or something, idk
            // TODO: double check for valid socket closing and closing other things
            if (!isValidAuth(authBuffer)) {
                System.out.println(clientSocket.getInetAddress() + " Client rejected, auth: " + new String(authBuffer));
                clientSocket.close();
                return;
            }

            // Assign player ID and send to the client
            playerId = getNextPlayerId();

            System.out.println("Assigned player Id: " + playerId);
            out.write(playerId);

            // Add to clients list
            ClientHandler clientHandler = new ClientHandler(playerId, clientSocket, in, out);
            clients.put(playerId, clientHandler);

            // Afterall initial setups done, wait for all 4 players to join before starting
            // the game
            waitUntilNoAvailableIds();

            clientHandler.handleMessages();
        } catch (Exception e) {
            // Add the assigned player id back into the list of available ones
            if (playerId != -1) {
                try {
                    addPlayerId(playerId);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
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
            // Do not send to the player who sent the original message
            if (client.playerId != sourcePlayerId) {
                client.out.write(message.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // FUNCTIONS FOR HANDLING PLAYER IDS
    // Function for threads (connected players) to wait until the list is empty
    public static void waitUntilNoAvailableIds() throws InterruptedException {
        synchronized (availablePlayerIds) {
            while (!availablePlayerIds.isEmpty()) {
                availablePlayerIds.wait(); // Wait until notified and list is empty
            }
        }
    }

    // Used for when a player disconnects before all 4 player joins
    // Returns a player id to the list of available ones
    private static synchronized void addPlayerId(int id) throws Exception {
        if (availablePlayerIds == null) {
            throw new IllegalStateException("Null list of available player ids");
        }
        boolean idAlreadyInList = availablePlayerIds.indexOf(id) != -1;
        if (idAlreadyInList) {
            throw new IllegalStateException("Player id to be added is already in the list");
        }
        availablePlayerIds.add(id);
    }

    // Thread-safe function for getting next player id
    private static synchronized int getNextPlayerId() throws IllegalStateException {
        if (availablePlayerIds == null) {
            throw new IllegalStateException("Null list of available player ids");
        }
        if (availablePlayerIds.size() == 0) {
            throw new IllegalStateException("No player Id available");
        }
        Integer temp = availablePlayerIds.remove(0);
        if (temp < 0 || temp > 3) {
            throw new IllegalStateException("Invalid player id generated: " + temp);
        }
        if (availablePlayerIds.isEmpty()) {
            availablePlayerIds.notifyAll();
        }
        return temp;
    }
}
