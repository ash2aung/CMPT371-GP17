package main.java.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 42042; // Random number, can be changed if needed
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    // TODO: What's happening with this? Send during first connection?
    // Khanh: after establishing connection, the client sends the VALID_AUTH string, in my test client I have it send "me key mause\n", using "\n" signal the end of the auth, but shouldn't be necessary
    private static final String SERVER_IP = "44.252.10.0"; //AWS VPS Pulic IP
    private static OutputStream os = 0;
    private static final int sendMovePacketSize = 2;
    private static int userId = -1;

    private byte[] buildPacket(int row, int col) {
        byte[] packet = new byte[sendMovePacketSize];
        byte moveToken = 0b100;

        // Assuming row and col will be 4b each => maze is 16x16
        packet[0] = (byte) ((moveToken << 5) | (row << 1) | ((col >> 3) & 0b00000001));
        packet[1] = (byte) ((col << 5));
        return packet;
    }

    private boolean validateMove(Char input) {
        int row; // Get row
        int col; // Get col
        MazeObject[][] maze = maze.getMaze();

        switch (Character.toLowerCase(e.getKeyChar())) {
            case 'w':
                if (maze[row - 1][col].isPassable())
                    return true;
            case 'a':
                if (maze[row][col - 1].isPassable())
                    return true;
            case 's':
                if (maze[row + 1][col].isPassable())
                    return true;
            case 'd':
                if (maze[row][col + 1].isPassable())
                    return true;
            default:
                return false;
        }
        return false;

    }

    public void sendInputToServer(char input, int currRow, int currCol) {
        if (!validateMove(input)) {
            return;
        }
        byte[] packet = buildPacket(currRow, currCol);

        try {
            os.write(packet);
            os.flush();
        } catch (IOException moveException) {
            System.out.println("Error sending movement message to server\n");
            moveException.printStackTrace();
        }
    }

    private static Runnable serverReceive(Socket socket) {
        return () -> {
            try {
                InputStream is = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                while (true) {
                    // Is readLine the best way to get the input?
                    String serverMsg = in.readLine();

                    // Handle input from server

                }
            } catch (IOException receiveException) {
                System.out.println("Error Receiving Packets\n");
                receiveException.printStackTrace();
            }
        };

        is.close();
    }

    public static void main(String args[]) throws IOException {
        Socket socket;

        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
        } catch (IOException socketException) {
            System.out.println("Issue in connecting\n");
            socketException.printStackTrace();
        }

        os = socket.getOutputStream();

        Thread sendThread = new Thread(serverReceive(socket)).start();

        os.close();
        socket.close();
    }
}
