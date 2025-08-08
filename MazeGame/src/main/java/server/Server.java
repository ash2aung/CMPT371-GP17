package server;

import game.PlayerMove;
import game.Cheese;
import game.Maze;
import game.MazeObject;
import game.Player;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
// Some basic setups for the server, will need to implement a lot of the server functions
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    // Mutex stuff
    private static final int numPlayers = 4;
    private static ReentrantLock movementLock;

    private static final int MAZE_SIDE = 20;
    private static final int MOVEPACKETSIZE = 3;
    private static final int PORT = 42042; // Random number, can be changed if needed
    private static ServerSocket serverSocket;
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static BlockingQueue<PlayerMove> moves;
    // Global queue used for handling player moves. Each client thread validate
    // moves then queues to this queue.
    private static int[] cheeseCoords = new int[2]; // I've decided that it's fine and better to keep cheeseCoords
    private static final int CHEESE_TO_WIN = 3;
    // Change: See line 23 and line 146
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

        System.out.println("Waiting for player connections...");
        // Loop for accepting 4 client connections
        while (availablePlayerIds.size() != 0) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Incoming connection attempt from " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch (Exception e) {
                e.printStackTrace();
                return; // early exit
            }
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
        maze.printMaze();
        // Broadcast maze to clients
        broadcastMazeToAllClients();

        // Game loop - process moves and handle game state
        System.out.println("Match start...");
        boolean gameActive = true;
        while (gameActive) {
            if (gameActive && !anyClientConnected()) {
                System.out.println("All clients disconnected, ending match early");
                gameActive = false;
                break;
            }
            // Process any queued moves (if using the queue approach)
            while (gameActive && !moves.isEmpty()) {
                PlayerMove move = moves.poll();
                if (move != null) {
                    System.out.println("Processing move from player " + move.getPlayerId());
                    // Validate and process move
                    // Update state of the maze
                    // Check for cheese and win
                    // If cheese eaten, place new cheese and do a cheese broadcast
                    // Etc

                    // If move is valid
                    // Change: Implemented
                    switch (validatePlayerMove(move)) {
                        // v for valid move
                        case ('v') -> {
                            broadcastPlayerMove(move);
                        }

                        // i for invalid move
                        case ('i') -> {
                            // Still must send the player's old move to indicate they haven't moved
                            broadcastInvalidMove(
                                    new PlayerMove(move.getPlayerId(), maze.getPlayers()[move.getPlayerId()].getRow(),
                                            maze.getPlayers()[move.getPlayerId()].getCol()));
                        }

                        // c for cheese found => valid move
                        case ('c') -> {
                            System.out.println("Cheese collected");
                            cheeseCoords = maze.placeCheeseRandomly();
                            broadcastCheeseCollection(move.getPlayerId(), move.getRow(), move.getCol(), cheeseCoords[0],
                                    cheeseCoords[1]);
                        }

                        // w for win => cheese found
                        case ('w') -> {
                            broadcastGameWin(move.getPlayerId());
                            gameActive = false; // Game stops
                            break;
                        }

                        default -> {
                            System.out.println("ERROR PROCESSING USER MOVE\n");
                        }
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                gameActive = false;
            }
        }
        // Clean up
        matchCleanup();
    }

    private static boolean anyClientConnected() {
        for (ClientHandler client : clients.values()) {
            if (client.socket.isClosed()) {
                continue; // This client is disconnected
            }

            // Test if the connection is actually alive with a ping
            try {
                byte[] pingPacket = new byte[4];
                pingPacket[0] = (byte) 0b11100000; // Token 0b111
                client.out.write(pingPacket);
                client.out.flush();
                return true; // At least one client responded to ping
            } catch (IOException e) {
                System.out.println("Client " + client.playerId + " failed ping test");
                // Continue checking other clients
            }
        }
        return false; // No clients connected or responsive
    }

    private static void matchCleanup() {
        System.out.println("Match ended, cleaning up...");

        for (ClientHandler client : clients.values()) {
            try {
                if (!client.socket.isClosed()) {
                    client.socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        clients.clear();

        System.out.println("Clients cleaned up, ready for new match");
    }

    // Sets states to starting defaults
    private static void matchInit() {
        // Create new random maze
        maze = new Maze();
        maze.printMaze();
        // Reset move queue
        moves = new LinkedBlockingQueue<>();
        // Reset available player Ids
        availablePlayerIds = new ArrayList<Integer>();
        // Change: next 2 lines used to say "PIDS". Now works with the locks
        for (int i = 0; i < numPlayers; i++) {
            availablePlayerIds.add(i);
        }
        System.out.println("Init available ids: " + availablePlayerIds);

        // Create a new lock
        movementLock = new ReentrantLock();

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
        } catch (IOException e) {
            // Add the assigned player id back into the list of available ones
            if (playerId != -1) {
                try {
                    addPlayerId(playerId);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            e.printStackTrace();
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

    /*
     * Return state of player move"
     * 'v': Valid move
     * 'i': Invalid move
     * 'c': Cheese collected
     * 'w': Third cheese collected => win
     */
    private static char validatePlayerMove(PlayerMove move) {
        Player currentPlayer = maze.getPlayers()[move.getPlayerId()];
        MazeObject temp = maze.getMaze()[move.getRow()][move.getCol()];

        // Lock mutex
        movementLock.lock();

        try {
            // Check the place the player's trying to move to
            boolean isCheese = move.getRow() == maze.getCheese().getRow() && move.getCol() == maze.getCheese().getCol();
            if (isCheese) {
                currentPlayer.addCheeseCount();
                if (currentPlayer.getCheeseCount() == CHEESE_TO_WIN) {
                    return 'w';
                }

                // Update player position internally
                maze.movePlayer(move.getPlayerId(), move.getRow(), move.getCol());

                return 'c';

            } else if (!temp.isPassable()) {
                // TODO: This can never be reached as client-side checks if the player is attempting to walk into a wall
                //  Remove this?

                System.out.println("Player at " + move.getRow() + ", " + move.getCol());
                return 'i';
            } else {
                System.out.println("Player has not collected cheese... ");
                if (maze.checkForPlayer(move.getPlayerId(), move.getRow(), move.getCol())) {
                    // Player collision!
                    System.out.println("And player collision detected!\n");
                    return 'i';
                }

                System.out.println("But player has made a valid move!\n");
                maze.movePlayer(move.getPlayerId(), move.getRow(), move.getCol());

                return 'v';
            }
        } finally {
            // Call unlock in the event of an exception, and after any call to return
            movementLock.unlock();
        }
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

    private static void broadcastInvalidMove(PlayerMove move) {
        int playerId = move.getPlayerId();
        int row = move.getRow();
        int col = move.getCol();

        byte[] movePacket = new byte[4];
            // Token: 0b010 (MOVE)
        movePacket[0] = (byte) (0b01000000 | ((playerId & 0b11) << 3) | ((row >> 2) & 0b111));
        movePacket[1] = (byte) (((row & 0b11) << 6) | ((col & 0b11111) << 1));
        movePacket[2] = 0; // Unused
        movePacket[3] = 0; // Unused

        broadcast(movePacket, -1);
    }

    private static void broadcastCheeseCollection(int playerId, int playerRow, int playerCol, int newCheeseRow,
            int newCheeseCol) {
        System.out.println("Broadcasting new Cheese");
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
        System.out.println("Broadcasting game win");
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
        final int MAZE_SIZE = MAZE_SIDE * MAZE_SIDE;
        final int PACKET_SIZE = MAZE_SIZE * 4 / 8; //
        byte[] mazePacket = new byte[PACKET_SIZE];
        // Process each tile in the maze
        System.out.println("Processing maze");
        for (int i = 0; i < MAZE_SIZE; i++) {
            int row = i / MAZE_SIDE;
            int col = i % MAZE_SIDE;

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
            int tileIndex = (cheeseR * MAZE_SIDE) + cheeseC; // Tile index (0-1023)
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

        public void handleMessages() throws IOException {
            byte[] input = new byte[MOVEPACKETSIZE];
            try {
                while (!socket.isClosed()) {
                    // Receive move packet from client
                    int bytesRead = 0;
                    while (bytesRead < MOVEPACKETSIZE) {
                        int result = in.read(input, bytesRead, MOVEPACKETSIZE - bytesRead);
                        if (result == -1) {
                            throw new IOException("Issue reading move packets from client");
                        }
                        bytesRead += result;
                    }
                    System.out.println("Received move from player " + playerId);

                    // Process into a PlayerMove object
                    PlayerMove move = processMovePacket(input);
                    System.out.println("Decoded into player move from player id: " + move.getPlayerId() + ", row: "
                            + move.getRow() + ", col: " + move.getCol());
                    // Add to the BlockingQueue
                    moves.add(move);
                }
            } catch (IOException e) {
                System.err.println("Client " + playerId + " disconnected: " + e.getMessage());
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
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
