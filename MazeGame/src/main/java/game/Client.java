package game;

import java.io.*;
import java.net.*;

public class Client {
    private static final int SERVER_PORT = 42042; // Random number, can be changed if needed
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final String SERVER_IP = "44.252.10.0"; // AWS VPS IP

    private static Socket socket;
    private static OutputStream os;
    private static InputStream is;
    private static PrintWriter out;
    private Thread receiveThread;
    private volatile boolean isConnected = false;

    private static final int MAZE_SIDE = 20;
    private static final int sendMovePacketSize = 3;
    private static final int receiveMazePacketSize = MAZE_SIDE * MAZE_SIDE * 4 / 8;
    private static final int receiveOtherPacketSize = 4;
    private static int userId = -1;

    private static final int MAZE_SIZE = MAZE_SIDE * MAZE_SIDE;

    private byte[] buildPacket(int row, int col) {
        byte[] packet = new byte[sendMovePacketSize];
        byte moveToken = 0b010; // bit encoding for token "MOVE"

        // Assuming row and col will be 5b each => maze is 20x20
        packet[0] = (byte) (((moveToken << 5) & 0b11100000) | ((userId << 3) & 0b00011000) | ((row >> 2) & 0b00000111));
        packet[1] = (byte) (((row << 6) & 0b11000000) | ((col << 1) & 0b00111110));
        return packet;
    }

    public void sendInputToServer(int newRow, int newCol) {
        System.out.println("Sending input to server");
        byte[] packet = buildPacket(newRow, newCol);

        try {
            os.write(packet);
            os.flush();
        } catch (IOException moveException) {
            System.out.println("Error sending movement message to server\n");
            moveException.printStackTrace();
        }
    }

    private boolean processOtherServerPacket(byte[] input) {
        int token = (input[0] >> 5) & 0b00000111;
        switch (token) {
            case 0b001: {
                // Start: Display maze
                break;
            }
            case 0b010: {
                // Move:
                int playerID = ((input[0] >> 3) & 0b00000011);
                int newRow = ((input[0] & 0b00000111) << 2) | ((input[1] >> 6) & 0b00000011);
                int newCol = ((input[1] >> 1) & 0b00011111);
                // moveUserFromID(playerID, newRow, newCol); CHECK IF ID = USER && THEY HAVEN'T
                // MOVED => server accepts their move
                // updateVisibilityAroundPlayer();
                // TODO: Talk to jack about updateVidibility being private and how to call it
                break;
            }
            case 0b011: {
                // A player collected the cheese
                int playerID = ((input[0] >> 3) & 0b00000011);
                int newPlayerRow = ((input[0] & 0b00000111) << 2) | ((input[1] >> 6) & 0b00000011);
                int newPlayerCol = ((input[1] >> 1) & 0b00011111);
                int newCheeseRow = ((input[1] & 0b00000001) << 4) | ((input[2] >> 4) & 0b00001111);
                int newCheeseCol = ((input[2] & 0b00001111) << 1) | ((input[3] >> 7) & 0b00000001);
                // moveUserFromID(playerID, newPlayerRow, newPlayerCol); CHECK IF ID = USER &&
                // THEY HAVEN'T MOVED => server accepts their move
                // updateVisibilityAroundPlayer();
                // placeNewCheese(newCheeseRow, newCheeseCol);
                break;
            }
            case 0b100: {
                // A player won
                int playerID = ((input[0] >> 3) & 0b00000011);
                // displayGameOverScreen(playerID);
                return false;
            }
            case 0b111: {
                // Ping packet, do nothing
                break;
            }
            default:
                System.out.println("ISSUE WITH INTERPRETING OTHER PACKET FROM SERVER'S TOKEN\n");
        }

        return true;
    }

    private Runnable serverReceive(Socket socket) {
        return () -> {
            try {
                byte[] input = new byte[receiveOtherPacketSize];

                while (isConnected && !Thread.currentThread().isInterrupted()) {
                    int bytesRead = 0;

                    while (bytesRead < receiveOtherPacketSize && isConnected) {
                        int result = is.read(input, bytesRead, receiveOtherPacketSize - bytesRead);
                        if (result == -1) {
                            System.out.println("Server connection closed");
                            isConnected = false;
                            return;
                        }
                        bytesRead += result;
                    }

                    if (isConnected) {
                        boolean continueLoop = processOtherServerPacket(input);
                        if (!continueLoop) {
                            isConnected = false;
                        }
                    }
                }

            } catch (IOException receiveException) {
                if (isConnected) { // Only log if we weren't intentionally disconnecting
                    System.out.println("Error Receiving Packets: " + receiveException.getMessage());
                }
            } finally {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Error closing socket in receive thread: " + e.getMessage());
                }
            }
        };
    }

    private static void sendInitToServer() {
        // Send auth to server proving
        out.write(VALID_AUTH);
        out.flush();
        // Get back the player ID
        try {
            byte[] userid = new byte[1];
            is.read(userid, 0, 1);
            System.out.println("USER ID: " + userid[0] + "\n");
            userId = userid[0];
        } catch (IOException readingException) {
            System.out.println("UNABLE TO GET PLAYER ID FROM SERVER\n");
            return;
        }
    }

    private static byte[] getMaze() throws IOException {
        System.out.println("Getting maze from server");
        byte[] mazeDescription = new byte[receiveMazePacketSize];
        int totalbytesRead = 0;

        while (totalbytesRead < mazeDescription.length) {
            int bytesRead = 0;
            try {
                bytesRead = is.read(mazeDescription, totalbytesRead, mazeDescription.length - totalbytesRead);
            } catch (IOException serverReadException) {
                System.out.println("SERVER GET MAZE EXCEPTION\n");
                serverReadException.printStackTrace();
                return null;
            }
            if (bytesRead == -1) {
                throw new IOException("Stream closed early\n");
            }
            totalbytesRead += bytesRead;
            System.out.println("Bytes read this iteration: " + bytesRead + ", Total: " + totalbytesRead + "/"
                    + mazeDescription.length);
        }

        return mazeDescription;
    }

    private static Maze createMaze(byte[] mazeDescription) {
        Maze maze = new Maze(true);
        // For each tile
        for (int i = 0; i < MAZE_SIZE; i++) {
            byte tileDescription = 0;
            // Get the correct half of the byte
            if (i % 2 == 0) {
                tileDescription = (byte) ((mazeDescription[i / 2] >> 4) & 0b00001111);
            } else {
                tileDescription = (byte) (mazeDescription[i / 2] & 0b00001111);
            }

            // Set current coords
            int row = i / MAZE_SIDE;
            int col = i % MAZE_SIDE;

            switch ((int) tileDescription) {
                case 0b0000: {
                    // Wall: Initially filled with all walls so do nothing
                    break;
                }
                case 0b0001: {
                    // Floor
                    maze.getMaze()[row][col].passable = true;
                    break;
                }
                case 0b0010: {
                    // Floor with wall decoration 1
                    maze.getMaze()[row][col].passable = true;
                    break;
                }
                case 0b0011: {
                    // Floor with wall decoration 2
                    maze.getMaze()[row][col].passable = true;
                    break;
                }
                case 0b0100: {
                    // Floor with floor decoration 1
                    maze.getMaze()[row][col].passable = true;
                    break;
                }
                case 0b0101: {
                    // Floor with floor decoration 2
                    maze.getMaze()[row][col].passable = true;
                    break;
                }
                case 0b0110: {
                    // Cheese
                    // TODO: Currently overriding decorations on this
                    // tile
                    maze.getMaze()[row][col].passable = true;
                    maze.placeCheeseAt(row, col);
                    break;
                }
                case 0b0111: // Player 1
                case 0b1000: // Player 2
                case 0b1001: // Player 3
                case 0b1010:
                    break; // Player 4
                default:
                    System.out.println("ERROR IN DECRYPTING SERVER'S PACKET\n");
            }
        }
        return maze;
    }

    private void closeConnections() throws IOException {
        os.close();
        out.close();
        is.close();
    }

    public Maze setupConnection() throws IOException {
        socket = null;
        try {
            // Set up I/O streams
            socket = new Socket(SERVER_IP, SERVER_PORT);
            os = socket.getOutputStream();
            out = new PrintWriter(os, true);
            is = socket.getInputStream();
            isConnected = true;
        } catch (IOException socketException) {
            System.out.println("Issue in connecting\n");
            socketException.printStackTrace();
            cleanup();
            return null;
        }

        System.out.println("Connected to server.");
        // Send setup msg
        sendInitToServer();

        // Get Maze (display waiting screen?)
        byte[] mazeDescription = null;
        try {
            mazeDescription = getMaze();
        } catch (IOException serverGetMazeException) {
            System.out.println("Error with getting maze from server\n");
            serverGetMazeException.printStackTrace();
            closeConnections();
            socket.close();
            return null;
        }

        // Process Maze
        Maze maze = createMaze(mazeDescription);
        maze.setUserId(userId);
        System.out.println("Created maze from description from Server");
        maze.printMaze();

        // Store reference to the thread so we can manage it
        receiveThread = new Thread(serverReceive(socket));
        receiveThread.setDaemon(true); // Make it a daemon thread
        receiveThread.start();

        return maze;
    }

    public void cleanup() {
        System.out.println("Starting client cleanup...");
        isConnected = false; // Signal threads to stop

        try {
            // Close streams first to unblock any reading operations
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
            if (out != null) {
                out.close();
            }

            // Close socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            // Wait for receive thread to finish (with timeout)
            if (receiveThread != null && receiveThread.isAlive()) {
                receiveThread.interrupt();
                try {
                    receiveThread.join(1000); // Wait up to 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Client cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
}
