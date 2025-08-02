package game;


import java.util.Random;

public class Maze {
    final static int NUM_OF_COLUMNS = 10;
    final static int NUM_OF_ROWS = 15;

    private MazeObject[][] maze;
    private Player[] players = new Player[4];


    // This is the id of the game player/client/user. So this is you.
    // It will be distributed  by the server at the start of the game
    private int userId = 0;


    public Maze () {
        // Create board
        maze = new MazeBuilder(NUM_OF_ROWS, NUM_OF_COLUMNS).getMaze();

        // Add players to board
        addPlayerToBoard(0, 1, 1);  // top left
        addPlayerToBoard(1, 1, NUM_OF_COLUMNS - 2); // top right
        addPlayerToBoard(2, NUM_OF_ROWS - 2, 1);     // bottom left
        addPlayerToBoard(3, NUM_OF_ROWS - 2, NUM_OF_COLUMNS - 2);

        printMaze();

    }

    private void addPlayerToBoard(int playerId, int row, int col) {
        players[playerId] = new Player(playerId, col, row);
        maze[row][col] = players[playerId];
        updateVisibilityAroundPlayer(players[playerId]);
    }


    /**
     * Temporary function. May be replaced when Canvas comes in.
     * Need to deal with lower and upper cases later
     * @param key It's the WASD key inputs
     */
    public void moveWithUserInput(char key) {
        Player user = players[userId];

        int userRow = user.getRow();
        int userCol = user.getCol();


        switch (key) {
            case 'w':
                processPlayerMove(userId, userRow - 1, userCol);    // up
                break;
            case 'a':
                processPlayerMove(userId, userRow, userCol - 1);    // left
                break;
            case 's':
                processPlayerMove(userId, userRow + 1, userCol);    // down
                break;
            case 'd':
                processPlayerMove(userId, userRow, userCol + 1);    // right
                break;
        }
    }

    /**
     * This function validates and process each player move. If invalid, do nothing
     * If valid, then move the player accordingly (by calling the relevant functions)
     * This method can be used by ANY/ALL players
     */
    public void processPlayerMove(int playerId, int row, int col) {
        MazeObject temp = maze[row][col];
        if (temp instanceof Cheese) {
            cheeseFound(playerId, row, col);

        } else if (!temp.isPassable()) {
            // this is a wall, deal with it
            System.out.println("Wall at " + row + ", " + col);

        } else {
            movePlayer(userId, row, col);
        }
    }

    private void cheeseFound(int playerId, int row, int col) {
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
     * This function does to need to validate whether the movement is valid. It assumes
     * that only validated PlayerMoves are given.
     */
    private void movePlayer(int playerId, int row, int col) {
//        updatePlayerPosition(players[playerId], col, row);
        updatePlayerPosition(players[playerId], row, col);
        updateVisibilityAroundPlayer(players[playerId]);
        notifyClientAboutUserMove();
        printMaze();
    }


    /**
     * Makes all directly adjacent squares visible to the player
     */
    private void updateVisibilityAroundPlayer(Player p) {
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
     * When there's a valid player move, this function is called. It updates
     * the position of player with playerId, and replaces the previous position
     * with a blank space
     * @param p The i
     * @param col
     * @param row
     */
    private void updatePlayerPosition(Player p, int row, int col) {
        maze[row][col] = p;

        maze[p.getRow()][p.getCol()] = new MazeObject(true);

        p.setRow(row);
        p.setCol(col);
    }

    public MazeObject[][] getMaze() {
        return maze;
    }

    public void printMaze() {
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                printMazeObject(maze[row][col]);
            }
            System.out.println();
        }

        // debug line
        System.out.println("You are at " + players[userId].getRow() + ", " + players[userId].getCol());
    }

    public void printMazeObject(MazeObject obj) {
        if (!obj.isVisible()) {
            System.out.print(". ");
        } else {
            if (obj instanceof Player) {
                System.out.print(obj + " ");
            } else if (obj instanceof Cheese) {
                System.out.print("X ");
            } else if (!obj.isPassable()) {
                System.out.print("# ");
            } else {
                System.out.print("  ");
            }
        }
    }

    public void revealEntireMaze() {
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLUMNS; col++) {
                maze[row][col].setVisible();
            }
        }
        printMaze();
    }

    // debug space

    // debug space end


    // The following code is for the server to use:

    /**
     * Right after generating a map, the server will also generate a cheese using
     * this method. Then it will distribute the Map and other elements to all players
     * Whenever a Cheese is verified to be eaten, the server will call this method again
     * to replace a new cheese.
     */
    public void placeCheeseRandomly() {
        int cheeseRow;
        int cheeseCol;
        Random random = new Random();

        while (true) {
            cheeseRow = random.nextInt(NUM_OF_ROWS);
            cheeseCol = random.nextInt(NUM_OF_COLUMNS);

            MazeObject temp = maze[cheeseRow][cheeseCol];
            // If not a wall or a player, place cheese there
            if (temp.isPassable() && !(temp instanceof Player)) {
                maze[cheeseRow][cheeseCol] = new Cheese(cheeseCol, cheeseRow);
                System.out.println("Cheese at " + cheeseRow + ", " + cheeseCol);
                break;
            }
        }
    }


    // The following methods are for Client code to use:

    /**
     * This method is called when the player (user) moves onto a cheese block. They
     * will call this method, and client programmer can fill out the code
     */
    public void notifyClientThatUserCollectedCheese() {}


    /**
     * This is when a player (user) move gets validated (no walls, or cheese). They
     * will be moving to that tile. This function will do some code that notifies the
     * Client, which will in turn do the necessary work and notify the server
     */
    public void notifyClientAboutUserMove() {}

}