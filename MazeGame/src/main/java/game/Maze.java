package game;


public class Maze {
    final static int NUM_OF_COLUMNS = 20;
    final static int NUM_OF_ROWS = 30;

    private MazeObject[][] maze;
    private Player[] players = new Player[4];


    // This is the id of the game player/client/user. So this is you.
    // It will be distributed  by the server at the start of the game
    private int userId = 0;


    public Maze () {
        // Create board
        maze = new MazeBuilder(NUM_OF_COLUMNS, NUM_OF_ROWS).getMaze();

        // Add players to board
        addPlayerToBoard(0, 1, 1);
        addPlayerToBoard(1, 18, 1);
        addPlayerToBoard(2, 1, 28);
        addPlayerToBoard(3, 18, 28);

        printMaze();

    }

    private void addPlayerToBoard(int playerId, int col, int row) {
        players[playerId] = new Player(playerId, col, row);
        maze[col][row] = players[playerId];
        updateVisibilityAroundPlayer(players[playerId]);
    }

    /**
     * It reads PlayerMove packets applies the moves as indicated in the packet.
     * This function does to need to validate whether the movement is valid. It assumes
     * that only validated PlayerMoves are given.
     */
    public void movePlayer(int playerId, int row, int col) {

        updatePlayerPosition(players[playerId], row, col);
        updateVisibilityAroundPlayer(players[playerId]);
        printMaze();
    }

    /**
     * Sets all spaces on the board to visible
     */
    public void revealMaze() {
        for (int col = 0; col < NUM_OF_COLUMNS; col++) {
            for (int row = 0; row < NUM_OF_ROWS; row++) {
                maze[col][row].setVisible();
            }
        }
    }

    /**
     * Temporary function. May be replaced when Canvas comes in.
     * Need to deal with lower and upper cases later
     * @param key It's the WASD key inputs
     */
    public void moveWithUserInput(char key) {
        Player user = players[userId];

        int userCol = user.getCol();
        int userRow = user.getRow();

        switch (key) {
            case 'w':
                processPlayerMove(userCol, userRow - 1);
                break;
            case 'a':
                processPlayerMove(userCol - 1, userRow);
                break;
            case 's':
                processPlayerMove(userCol, userRow + 1);
                break;
            case 'd':
                processPlayerMove(userCol + 1, userRow);
                break;
        }
    }

    /**
     * This function validates and process each player move. If invalid, do nothing
     * If valid, then move the player accordingly (by calling the relevant functions)
     */
    private void processPlayerMove(int col, int row) {
        MazeObject temp = maze[col][row];
        if (temp instanceof Cheese) {
            // this is a cheese, deal with it
        } else if (!temp.isPassable()) {
            // this is a wall, deal with it
            System.out.println("Wall at " + col + ", " + row);
        } else {
            movePlayer(userId, row, col);
        }
    }


    /**
     * Makes all directly adjacent squares visible to the player
     */
    private void updateVisibilityAroundPlayer(Player p) {
        int playerCol = p.getCol();
        int playerRow = p.getRow();
        maze[playerCol - 1][playerRow + 1].setVisible(); // Top left
        maze[playerCol][playerRow + 1].setVisible(); // Top mid
        maze[playerCol + 1][playerRow + 1].setVisible(); // Top right
        maze[playerCol - 1][playerRow].setVisible(); // Mid left
        maze[playerCol + 1][playerRow].setVisible(); // Mid right
        maze[playerCol - 1][playerRow - 1].setVisible(); // Bot left
        maze[playerCol][playerRow - 1].setVisible(); // Bot mid
        maze[playerCol + 1][playerRow - 1].setVisible(); // Bot right
    }

    /**
     * When there's a valid player move, this function is called. It updates
     * the position of player with playerId, and replaces the previous position
     * with a blank space
     * @param p The i
     * @param col
     * @param row
     */
    private void updatePlayerPosition(Player p, int col, int row) {
        maze[col][row] = p;

        maze[p.getCol()][p.getRow()] = new MazeObject(true);

        p.setCol(col);
        p.setRow(row);
    }

    public MazeObject[][] getMaze() {
        return maze;
    }

    public void printMaze() {
        for (int col = 0; col < NUM_OF_COLUMNS; col++) {
            for (int row = 0; row < NUM_OF_ROWS; row++) {
                printMazeObject(maze[col][row]);
            }
            System.out.println();
        }
    }

    public void printMazeObject(MazeObject obj) {
        if (!obj.isVisible()) {
            System.out.print(". ");
        } else {
            if (obj instanceof Player) {
                System.out.print(obj + " ");
            } else if (obj instanceof Cheese) {
                System.out.print("O ");
            } else if (!obj.isPassable()) {
                System.out.print("# ");
            } else {
                System.out.print("  ");
            }
        }
    }

    public void revealEntireMaze() {
        for (int col = 0; col < NUM_OF_COLUMNS; col++) {
            for (int row = 0; row < NUM_OF_ROWS; row++) {
                maze[col][row].setVisible();
            }
        }
        printMaze();
    }

}