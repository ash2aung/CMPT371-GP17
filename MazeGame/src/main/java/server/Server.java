package server;

import game.PlayerMove;
import game.Cheese;
import game.Maze;
import game.MazeObject;
import game.Player;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
// Some basic setups for the server, will need to implement a lot of the server functions
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private static final int MOVEPACKETSIZE = 3;
    private static final int PORT = 42042; // Random number, can be changed if needed
    private static ServerSocket serverSocket;
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static BlockingQueue<PlayerMove> moves;
    // Global queue used for handling player moves. Each client thread validate
    // moves then queues to this queue.
    private static int[] cheeseCoords = new int[2];
    private static final int[] PIDS = { 0, 1, 2, 3 }; // Player ids
    private static List<Integer> availablePlayerIds;
    private static Maze maze;
    // Match state
    private static int[] score = new int[4];

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

    private static void launchMatch() throws Exception {
        matchInit();
        try {
            serverSocket.setSoTimeout(2000); // 1 second timeout
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // Loop for accepting 4 client connections
        while (availablePlayerIds.size() != 0) {
            try {
                System.out.println("Waiting for more connection");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Incoming connection attempt from " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch (Exception e) {
                e.printStackTrace();
                return; // early exit
            }
            System.out.println("list size = " + availablePlayerIds.size());
        }

        System.out.println("4 players connected.");
        synchronized (availablePlayerIds) {
            if (availablePlayerIds.size() == 0) {
                availablePlayerIds.notifyAll();
            }
        }
        // Start the game
        // Place a cheese
        cheeseCoords = maze.placeCheeseRandomly();
        System.out.println("Cheese coords: " + cheeseCoords);
        // Broadcast maze to clients
        broadcastMazeToAllClients();

        // Game loop - process moves and handle game state
        boolean gameActive = true;
        while (gameActive) {
            // Process any queued moves (if using the queue approach)
            while (!moves.isEmpty()) {
                PlayerMove move = moves.poll();
                if (move != null) {
                    // Validate and process move
                    // Update state of the maze
                    // Check for cheese and win
                    // If cheese eaten, place new cheese and do a cheese broadcast
                    // Etc

                    // If move is valid
                    broadcastPlayerMove(move);
                }
            }
            // Check for win conditions, disconnections, etc.
            // More game logic if needed
        }

    }

    // Sets states to starting defaults
    private static void matchInit() {
        // Create new random maze
        maze = new Maze();
        // Reset move queue
        moves = new LinkedBlockingQueue<>();
        // Reset available player Ids
        availablePlayerIds = new ArrayList<Integer>();
        for (int i = 0; i < PIDS.length; i++) {
            availablePlayerIds.add(PIDS[i]);
        }
        System.out.println("Init available ids: " + availablePlayerIds);
        // Cheese removed
        cheeseCoords[0] = -1;
        cheeseCoords[1] = -1;
        // Reset score
        for (int i = 0; i < score.length; i++) {
            score[i] = 0;
        }
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
            System.out.println("Available ids: " + availablePlayerIds);

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

    private static void broadcastPlayerMove(PlayerMove move) {
        int playerId = move.getPlayerId();
        int row = move.getRow();
        int col = move.getCol();

        byte[] movePacket = new byte[4];
        // Token: 0b010 (MOVE)
        movePacket[0] = (byte) (0b01000000 | ((playerId & 0b11) << 3) | ((row >> 2) & 0b111));
        movePacket[1] = (byte) (((row & 0b11) << 6) | ((col & 0b11111) << 1));
        movePacket[2] = 0; // Unused
        movePacket[3] = 0; // Unused

        broadcast(movePacket, playerId);
    }

    private static void broadcastCheeseCollection(int playerId, int playerRow, int playerCol, int newCheeseRow,
            int newCheeseCol) {
        byte[] cheesePacket = new byte[4];
        // Token: 0b011 (CHEESE_COLLECTED)
        cheesePacket[0] = (byte) (0b01100000 | ((playerId & 0b11) << 3) | ((playerRow >> 2) & 0b111));
        cheesePacket[1] = (byte) (((playerRow & 0b11) << 6) | ((playerCol & 0b11111) << 1)
                | ((newCheeseRow >> 4) & 0b1));
        cheesePacket[2] = (byte) (((newCheeseRow & 0b1111) << 4) | ((newCheeseCol >> 1) & 0b1111));
        cheesePacket[3] = (byte) (((newCheeseCol & 0b1) << 7));

        broadcast(cheesePacket, -1); // Send to all players
    }

    private static void broadcastGameWin(int playerId) {
        byte[] winPacket = new byte[4];
        // Token: 0b100 (GAME_WIN)
        winPacket[0] = (byte) (0b10000000 | ((playerId & 0b11) << 3));
        winPacket[1] = 0;
        winPacket[2] = 0;
        winPacket[3] = 0;

        broadcast(winPacket, -1); // Send to all players
    }

    private static void broadcast(byte[] packet, int excludePlayerId) {
        for (ClientHandler client : clients.values()) {
            if (excludePlayerId == -1 || client.playerId != excludePlayerId) {
                try {
                    client.out.write(packet);
                    client.out.flush();
                } catch (Exception e) {
                    System.err.println("Failed to broadcast to client " + client.playerId);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void broadcastMazeToAllClients() {
        System.out.println("started broadcasting maze to clients");
        byte[] mazePacket = processMaze(maze.getMaze());

        // Send the maze packet to the clients
        for (ClientHandler client : clients.values()) {
            System.out.println("Sending maze to client: " + client.getId());
            try {
                client.out.write(mazePacket);
                client.out.flush();
                System.out.println("Sent maze to client id: " + client.getId());
            } catch (Exception e) {
                System.err.println("Failed to send maze to client " + client.playerId);
                e.printStackTrace();
            }
        }
    }

    // Processes the maze
    private static byte[] processMaze(MazeObject[][] mazeGrid) {
        final int MAZE_SIZE = 32 * 32;
        final int PACKET_SIZE = MAZE_SIZE * 4 / 8; // 512 bytes
        byte[] mazePacket = new byte[PACKET_SIZE];
        // Process each tile in the maze
        System.out.println("Processing maze");
        for (int i = 0; i < MAZE_SIZE; i++) {
            int row = i / 32;
            int col = i % 32;

            // Get the maze object at this position
            MazeObject obj = mazeGrid[row][col];

            // Encode the tile based on its type
            byte tileEncoding = encodeTile(obj);

            // Pack two tiles into each byte
            int byteIndex = i / 2;
            if (i % 2 == 0) {
                // First tile goes in the upper 4 bits
                mazePacket[byteIndex] = (byte) ((tileEncoding << 4) & 0b11110000);
            } else {
                // Second tile goes in the lower 4 bits
                mazePacket[byteIndex] |= (byte) (tileEncoding & 0b00001111);
            }
        }

        if (cheeseCoords[0] >= 0 && cheeseCoords[1] >= 0) {
            int cheeseR = cheeseCoords[0];
            int cheeseC = cheeseCoords[1];
            int tileIndex = (cheeseR * 32) + cheeseC; // Tile index (0-1023)
            int byteIndex = tileIndex / 2; // Byte index (0-511)

            byte temp = mazePacket[byteIndex]; // Get existing byte

            if (tileIndex % 2 == 0) {
                // Cheese in upper 4 bits, preserve lower 4 bits
                mazePacket[byteIndex] = (byte) ((0b0110 << 4) | (temp & 0b00001111));
            } else {
                // Cheese in lower 4 bits, preserve upper 4 bits
                mazePacket[byteIndex] = (byte) ((temp & 0b11110000) | 0b0110);
            }
        } else {
            throw new IllegalStateException("No cheese");
        }
        return mazePacket;
    }

    // Encodes the tile into 4-bit
    private static byte encodeTile(MazeObject obj) {
        if (!obj.isPassable()) {
            return (byte) 0b0000; // Wall
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            int playerId = player.getId();
            return (byte) (0b0111 + playerId); // 0b0111 for player 0, 0b1000 for player 1, etc.
        } else {
            return (byte) 0b0001; // Floor
        }
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

        public void handleMessages() throws Exception {
            byte[] input = new byte[MOVEPACKETSIZE];
            while (true) {
                // Receive move packet from client
                int bytesRead = 0;
                while (bytesRead < MOVEPACKETSIZE) {
                    int result = in.read(input, bytesRead, MOVEPACKETSIZE - bytesRead);
                    if (result == -1) {
                        throw new IOException("Issue reading move packets from client");
                    }
                    bytesRead += result;
                }

                // Process into a PlayerMove object
                PlayerMove move = processMovePacket(input);

                // Add to the BlockingQueue
                moves.add(move);
            }
        }

        private PlayerMove processMovePacket(byte[] input) {
            int token = (input[0] >> 5) & 0b00000111;
            if (token != 0b010) {
                throw new IllegalArgumentException("Invalid move token: " + token);
            }
            int playerId = ((input[0] >> 3) & 0b00000011);
            int newRow = ((input[0] & 0b00000111) << 2) | ((input[1] >> 6) & 0b00000011);
            int newCol = ((input[1] >> 1) & 0b00011111);
            if (playerId != this.playerId) {
                throw new IllegalStateException("Invalid player id: " + playerId);
            }
            return new PlayerMove(playerId, newRow, newCol);
        }

        // Function for validating a player move
        // private static boolean validateMove(PlayerMove move) {
        // int id = move.getPlayerId();
        // int row = move.getRow();
        // int col = move.getCol();
        // // Check if the move is within maze bounds
        // if (row < 0 || row >= 32 || col < 0 || col >= 32) {
        // return false;
        // }

        // // Check if the destination tile is passable
        // MazeObject targetTile = maze.getMaze()[row][col];
        // if (!targetTile.isPassable()) {
        // return false;
        // }

        // // check if move is adjacent to current position
        // Player player = maze.getPlayers()[id];
        // int currRow = player.getRow();
        // int currCol = player.getCol();
        // if (Math.abs(row - currRow) > 1) {
        // System.err.println("Illegal move: prev row = " + currRow + ", new row = " +
        // row);
        // return false;
        // } else if (Math.abs(col - currCol) > 1) {
        // System.err.println("Illegal move: prev col = " + currCol + ", new col = " +
        // col);
        // return false;
        // }

        // return true;
        // }

        public int getId() {
            return playerId;
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
    private static int getNextPlayerId() throws IllegalStateException {
        if (availablePlayerIds == null) {
            throw new IllegalStateException("Null list of available player ids");
        }
        synchronized (availablePlayerIds) {
            if (availablePlayerIds.size() == 0) {
                throw new IllegalStateException("No player Id available");
            }
            Integer temp = availablePlayerIds.remove(0);
            if (temp < 0 || temp > 3) {
                throw new IllegalStateException("Invalid player id generated: " + temp);
            }
            if (availablePlayerIds.size() == 0) {
                availablePlayerIds.notifyAll();
            }
            System.out.println("Available ids: " + availablePlayerIds);
            return temp;
        }
    }
}
