package game;

import java.util.Random;

public class Maze {
    final static int NUM_OF_COLUMNS = 20;
    final static int NUM_OF_ROWS = 20;

    private MazeObject[][] maze;
    private Player[] players = new Player[4];
    private Cheese cheese;

    // This is the id of the game player/client/user. So this is you.
    // It will be distributed by the server at the start of the game
    private int userId = 0;

    public Maze() {
        // Create board
        maze = new MazeBuilder(NUM_OF_ROWS, NUM_OF_COLUMNS).getMaze();
        revealBorders();

        // Add players to board
        addPlayerToBoard(0, 1, 1); // top left
        addPlayerToBoard(1, 1, NUM_OF_COLUMNS - 2); // top right
        addPlayerToBoard(2, NUM_OF_ROWS - 2, 1); // bottom left
        addPlayerToBoard(3, NUM_OF_ROWS - 2, NUM_OF_COLUMNS - 2);
        removeWallsAroundPlayers();

    }

    private void addPlayerToBoard(int playerId, int row, int col) {
        players[playerId] = new Player(playerId, col, row);
        updateVisibilityAroundPlayer(playerId);
    }

    public Maze(boolean serverGiven) {
        // Default constructor where the maze is populated via server

        // Fill maze entirely with walls
        this.maze = new MazeObject[NUM_OF_ROWS][NUM_OF_COLUMNS];
        for (int i = 0; i < NUM_OF_ROWS; i++) {
            for (int j = 0; j < NUM_OF_COLUMNS; j++) {
                this.maze[i][j] = new MazeObject(false);
            }
        }

        // Add players to board
        addPlayerToBoard(0, 1, 1); // top left
        addPlayerToBoard(1, 1, NUM_OF_COLUMNS - 2); // top right
        addPlayerToBoard(2, NUM_OF_ROWS - 2, 1); // bottom left
        addPlayerToBoard(3, NUM_OF_ROWS - 2, NUM_OF_COLUMNS - 2);
    }

    /**
     * Temporary function. May be replaced when Canvas comes in.
     * Need to deal with lower and upper cases later
     *
     * @param key It's the WASD key inputs
     */
    public void moveWithUserInput(char key) {
        Player user = players[userId];

        int userRow = user.getRow();
        int userCol = user.getCol();

        switch (key) {
            case 'w':
                processPlayerMove(userId, userRow - 1, userCol); // up
                break;
            case 'a':
                processPlayerMove(userId, userRow, userCol - 1); // left
                break;
            case 's':
                processPlayerMove(userId, userRow + 1, userCol); // down
                break;
            case 'd':
                processPlayerMove(userId, userRow, userCol + 1); // right
                break;
        }
    }

    // Used with client
    public void moveWithUserInput(char key, Client client) {
        Player user = players[userId];

        int userRow = user.getRow();
        int userCol = user.getCol();

        switch (key) {
            case 'w':
                processPlayerMove(userId, userRow - 1, userCol, client); // up
                break;
            case 'a':
                processPlayerMove(userId, userRow, userCol - 1, client); // left
                break;
            case 's':
                processPlayerMove(userId, userRow + 1, userCol, client); // down
                break;
            case 'd':
                processPlayerMove(userId, userRow, userCol + 1, client); // right
                break;
        }
    }

    /**
     * This function validates and process each player move. If invalid, do nothing
     * If valid, then move the player accordingly (by calling the relevant
     * functions)
     * This method can be used by ANY/ALL players
     */
    public void processPlayerMove(int playerId, int row, int col) {
        MazeObject temp = maze[row][col];
        if (checkForPlayer(playerId, row, col)) {
            // this is a player, deal with it
            System.out.println("Collision at " + row + ", " + col);

        } else if (checkForCheese(row, col)) {
            cheeseFound(playerId, row, col);

        } else if (!temp.isPassable()) {
            // this is a wall, deal with it
            System.out.println("Wall at " + row + ", " + col);

        } else {
            movePlayer(userId, row, col);
        }
    }

    /**
     * This method will take in a row and col, and check if that cell has another
     * player
     * 
     * @param playerId The player we want to check for
     * @param row      Row number
     * @param col      Column number
     * @return True if collision, false otherwise
     */
    private boolean checkForPlayer(int playerId, int row, int col) {

        for (int i = 0; i < 4; i++) {
            if (i == playerId) {
                continue;
            }
            int otherPlayerRow = players[i].getRow();
            int otherPlayerCol = players[i].getCol();

            // check for collision
            if (row == otherPlayerRow && col == otherPlayerCol) {
                return true;
            }
        }
        return false;
    }

    private boolean checkForCheese(int row, int col) {
        if (cheese == null) {
            return false;
        }
        return (row == cheese.getRow() && col == cheese.getCol());
    }

    // Used with the client
    public void processPlayerMove(int playerId, int row, int col, Client client) {
        MazeObject temp = maze[row][col];
        if (temp instanceof Cheese) {
            cheeseFound(playerId, row, col);

        } else if (!temp.isPassable()) {
            // this is a wall, deal with it
            System.out.println("Wall at " + row + ", " + col);

        } else {
            movePlayer(userId, row, col);
            client.sendInputToServer(row, col);
        }
    }

    private void cheeseFound(int playerId, int row, int col) {
        placeCheeseRandomly(); // temporary method for game testing. REMOVE IT AFTERWARD

        if (playerId == userId) {
            notifyClientThatUserCollectedCheese();

            // then add a line of code to increment cheese if verified
            players[playerId].addCheeseCount();
            movePlayer(playerId, row, col);

        } else {
            players[playerId].addCheeseCount();
            movePlayer(playerId, row, col);
        }

        System.out.println("Cheese claimed by player " + playerId);
        System.out.println("Player " + playerId + " has "
                + players[playerId].getCheeseCount() + " cheese.");

    }

    /**
     * It reads PlayerMove packets applies the moves as indicated in the packet.
     * This function does to need to validate whether the movement is valid. It
     * assumes
     * that only validated PlayerMoves are given.
     */
    public void movePlayer(int playerId, int row, int col) {
        updatePlayerPosition(playerId, row, col);
        updateVisibilityAroundPlayer(playerId);
        notifyClientAboutUserMove(); // TODO: DELETE ME
        printMaze();
    }

    /**
     * Makes all directly adjacent squares visible to the player
     */
    private void updateVisibilityAroundPlayer(int playerId) {
        Player p = players[playerId];
        int playerRow = p.getRow();
        int playerCol = p.getCol();

        maze[playerRow - 1][playerCol - 1].setVisible(); // Top left
        maze[playerRow - 1][playerCol].setVisible(); // Top mid
        maze[playerRow - 1][playerCol + 1].setVisible(); // Top right
        maze[playerRow][playerCol - 1].setVisible(); // Mid left
        maze[playerRow][playerCol + 1].setVisible(); // Mid right
        maze[playerRow + 1][playerCol - 1].setVisible(); // Bot left
        maze[playerRow + 1][playerCol].setVisible(); // Bot mid
        maze[playerRow + 1][playerCol + 1].setVisible(); // Bot right
    }

    /**
     * This method is called when a move is validated. It will move select player
     * to target row and col
     * 
     * @param playerId ID of the player we want to move
     * @param row      Target row of the intended move
     * @param col      Target col of the intended move
     */
    private void updatePlayerPosition(int playerId, int row, int col) {
        players[playerId].setRow(row);
        players[playerId].setCol(col);
    }

    public void printMaze() {
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                printMazeObject(row, col);
            }
            System.out.println();
        }

        // debug line
        System.out.println("You are at " + players[userId].getRow() + ", " + players[userId].getCol());
    }

    public void printMazeObject(int row, int col) {
        MazeObject obj = maze[row][col];
        // here, we use -1 because no playerId can be -1. SO this will check for ALL
        // players
        if (checkForPlayer(-1, row, col)) {
            System.out.print(getPlayerWithRowCol(row, col) + " ");

        } else if (checkForCheese(row, col)) {
            System.out.print("X ");

        } else if (!obj.isVisible()) {
            System.out.print(". ");

        } else {
            if (!obj.isPassable()) {
                System.out.print("# ");
            } else {
                System.out.print("  ");
            }
        }
    }

    /**
     * Gets the player, given row and col. Use ONLY if player is confirmed to be in
     * that position
     * Otherwise, it'll break the code
     * 
     * @param row Target row to find Player
     * @param col Target col to find player
     * @return A player object at row, col
     * 
     */
    private Player getPlayerWithRowCol(int row, int col) {
        for (int i = 0; i < 4; i++) {
            int playerRow = players[i].getRow();
            int playerCol = players[i].getCol();

            if (playerRow == row && playerCol == col) {
                return players[i];
            }
        }
        return null;
    }

    public void revealEntireMaze() {
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                maze[row][col].setVisible();
            }
        }
        printMaze();
    }

    private void revealBorders() {
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                if (row == 0 || row == NUM_OF_ROWS - 1) {
                    maze[row][col].setVisible();
                } else if (col == 0 || col == NUM_OF_COLUMNS - 1) {
                    maze[row][col].setVisible();
                }
            }
        }
    }

    private void removeWallsAroundPlayers() {
        int row, col;
        // player 0, starting at top left corner
        row = players[0].getRow();
        col = players[0].getCol();
        maze[row][col + 1] = new MazeObject(true, true); // right
        maze[row + 1][col] = new MazeObject(true, true); // bottom
        maze[row + 1][col + 1] = new MazeObject(true, true); // bottom right

        // player 1, starting at top right corner
        row = players[1].getRow();
        col = players[1].getCol();
        maze[row][col - 1] = new MazeObject(true, true); // left
        maze[row + 1][col] = new MazeObject(true, true); // bottom
        maze[row + 1][col - 1] = new MazeObject(true, true); // bottom left

        // player 2, starting at bottom left corner
        row = players[2].getRow();
        col = players[2].getCol();
        maze[row - 1][col] = new MazeObject(true, true); // top
        maze[row][col + 1] = new MazeObject(true, true); // right
        maze[row - 1][col + 1] = new MazeObject(true, true); // top right

        // player 3, starting at bottom right corner
        row = players[3].getRow();
        col = players[3].getCol();
        maze[row - 1][col] = new MazeObject(true, true); // top
        maze[row][col - 1] = new MazeObject(true, true); // left
        maze[row - 1][col - 1] = new MazeObject(true, true); // top left

    }

    // getters

    public MazeObject[][] getMaze() {
        return maze;
    }

    public Player[] getPlayers() {
        return players;
    }

    public Cheese getCheese() {
        return cheese;
    }

    // debug space

    // debug space end

    // The following code is for the server to use:

    /**
     * Right after generating a map, the server will also generate a cheese using
     * this method. Then it will distribute the Map and other elements to all
     * players
     * Whenever a Cheese is verified to be eaten, the server will call this method
     * again
     * to replace a new cheese.
     */
    public int[] placeCheeseRandomly() {
        int[] ret = new int[2];
        int cheeseRow;
        int cheeseCol;
        Random random = new Random();

        while (true) {
            cheeseRow = random.nextInt(NUM_OF_ROWS);
            cheeseCol = random.nextInt(NUM_OF_COLUMNS);

            MazeObject temp = maze[cheeseRow][cheeseCol];
            // If not a wall or a player, place cheese there
            if (temp.isPassable() && !checkForPlayer(-1, cheeseRow, cheeseCol)) {
                cheese = new Cheese(cheeseCol, cheeseRow);
                System.out.println("Cheese at " + cheeseRow + ", " + cheeseCol);
                ret[0] = cheeseRow;
                ret[1] = cheeseCol;
                return ret;
            }
        }
    }

    public void placeCheeseAt(int row, int col) {
        cheese = new Cheese(col, row);
    }

    // The following methods are for Client code to use:

    /**
     * This method is called when the player (user) moves onto a cheese block. They
     * will call this method, and client programmer can fill out the code
     */
    public void notifyClientThatUserCollectedCheese() {
    }

    /**
     * This is when a player (user) move gets validated (no walls, or cheese). They
     * will be moving to that tile. This function will do some code that notifies
     * the
     * Client, which will in turn do the necessary work and notify the server
     */
    public void notifyClientAboutUserMove() {
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

}