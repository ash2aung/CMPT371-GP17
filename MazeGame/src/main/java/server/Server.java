package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 42042; // Random number, can be changed if needed
    private static final String VALID_AUTH = "me key mause"; // just an arbitrary string
    private static final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    static class ClientHandler {
        private final int userId;
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        public ClientHandler(int userId, Socket socket, BufferedReader in, PrintWriter out) {
            this.userId = userId;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        public void handleMessages() throws IOException {
            // placeholder
        }
    }
}
