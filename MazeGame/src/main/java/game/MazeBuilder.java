package game;

import java.util.*;

/**
 * This class creates a randomized maze, done in the follow steps :
 * Creates a 2d array full of walls except for the four corners which are set as
 * open spaces.
 * Generates the maze using the randomized depth first traversal algorithm.
 * After the maze is generated, 12% of the walls is removed to add cycles and
 * allow more paths in the maze.
 * The depth first traversal algorithm will prevent non-connected open cells and
 * prevent both 2x2 squares of open cells
 * and 2x2 squares of walls.
 */
public final class MazeBuilder {

    private final int rows;
    private final int columns;
    private MazeObject[][] maze;

    private final int MIN_ROWS = 4;
    private final int MIN_COLUMNS = 4;

    private MazeBuilder() {
        // Java FORCES the following two lines.
        this.columns = 0;
        this.rows = 0;
    }

    public MazeBuilder(int columns, int rows) {
        if (columns < MIN_COLUMNS || rows < MIN_ROWS) {
            throw new IllegalArgumentException(
                    "MazeBuilder was intended to create at least a " + MIN_ROWS + "x" + MIN_COLUMNS + " maze.");
        }

        this.columns = columns;
        this.rows = rows;

        generateMaze();
    }

    public MazeObject[][] getMaze() {
        return this.maze;
    }

    private void generateMaze() {
        initializeMaze();
        randomizedDepthFirstTraversal();
        randomlyRemoveWalls();
        // initializeMazeWithAllPassable();
    }

    private void initializeMazeWithAllPassable() {
        this.maze = new MazeObject[this.columns][this.rows];

        for (int i = 0; i < this.columns; i++) {
            for (int j = 0; j < this.rows; j++) {
                this.maze[i][j] = new MazeObject(true);
            }
        }
    }

    /**
     * Can safely remove walls if there are two other parallel walls around it.
     * Safely meaning will not result in 2x2 open square.
     */
    private void randomlyRemoveWalls() {
        // The number of randomly walls removed is 12% of the total number of blocks,
        // not including the outer wall
        int numOfWallsToRemove = (int) ((this.columns - 2) * (this.rows - 2) * 0.12);

        // add all walls to list
        List<MazeObject> walls = new ArrayList<MazeObject>((this.columns - 2) * (this.rows - 2));
        for (int i = 1; i < this.columns - 1; i++) {
            for (int j = 1; j < this.rows - 1; j++) {
                if (!this.maze[i][j].isPassable()) {
                    walls.add(this.maze[i][j]);
                }
            }
        }
        Collections.shuffle(walls);

        int i = 0;
        int wallsRemoved = 0;

        while (wallsRemoved != numOfWallsToRemove) {
            MazeObject wall = walls.get(i);

            // Remove wall if removable
            if (isRemovable(wall)) {
                wall.setPassable(true);
                wallsRemoved++;
            }

            i++;
        }
    }

    /**
     * Checks if there are parallel walls around the given wall.
     */
    private boolean isRemovable(MazeObject wall) {
        int currX = wall.getCol();
        int currY = wall.getRow();

        // check for parallel walls left and right
        if (!this.maze[currX + 1][currY].isPassable() && !this.maze[currX - 1][currY].isPassable()) {
            return true;
        }

        // check for parallel walls above and below
        if (!this.maze[currX][currY + 1].isPassable() && !this.maze[currX][currY - 1].isPassable()) {
            return true;
        }

        return false;
    }

    /**
     * Creates a maze full of walls.
     */
    private void initializeMaze() {
        this.maze = new MazeObject[this.columns][this.rows];

        for (int i = 0; i < this.columns; i++) {
            for (int j = 0; j < this.rows; j++) {
                this.maze[i][j] = new MazeObject(i, j, false);
            }
        }
        this.maze[1][1].setPassable(true);
        this.maze[1][this.rows - 2].setPassable(true);
        this.maze[this.columns - 2][this.rows - 2].setPassable(true);
        this.maze[this.columns - 2][1].setPassable(true);
    }

    /**
     * Use depth first traversal to create a maze.
     *
     * If we imagine the maze as a graph, let each vertex be an open cell and
     * is initially surrounded by 4 walls.
     *
     * Connect vertices by removing the wall between vertices.
     *
     * Algorithm is from https://en.wikipedia.org/wiki/Maze_generation_algorithm .
     */
    private void randomizedDepthFirstTraversal() {
        HashSet<MazeObject> visited = new HashSet<>();

        Stack<MazeObject> stack = new Stack<>();

        // (1,1) is starting location of player
        stack.push(this.maze[1][1]);
        visited.add(this.maze[1][1]);

        while (!stack.empty()) {

            MazeObject currentPoint = stack.pop();

            MazeObject nonVisitedAdjacentPoint = getRandomNonVisitedAdjacentPoint(currentPoint, visited);

            if (nonVisitedAdjacentPoint != null) {
                stack.push(currentPoint);

                // Remove wall between current and adjacent point
                removeWallBetweenPoints(currentPoint, nonVisitedAdjacentPoint);
                nonVisitedAdjacentPoint.setPassable(true);

                visited.add(nonVisitedAdjacentPoint);
                stack.push(nonVisitedAdjacentPoint);
            }
        }
    }

    private MazeObject getRandomNonVisitedAdjacentPoint(MazeObject currentPoint, HashSet<MazeObject> visited) {
        final int distance = 2;
        final int currX = currentPoint.getCol();
        final int currY = currentPoint.getRow();
        final int maxNumOfAdjacentPaths = 4;

        ArrayList<MazeObject> nonVisitedAdjacentPoints = new ArrayList<>(maxNumOfAdjacentPaths);

        // right
        if (validPoint(currX + distance, currY) && !visited.contains(this.maze[currX + distance][currY])) {
            nonVisitedAdjacentPoints.add(this.maze[currX + distance][currY]);
        }
        // left
        if (validPoint(currX - distance, currY) && !visited.contains(this.maze[currX - distance][currY])) {
            nonVisitedAdjacentPoints.add(this.maze[currX - distance][currY]);
        }
        // up
        if (validPoint(currX, currY + distance) && !visited.contains(this.maze[currX][currY + distance])) {
            nonVisitedAdjacentPoints.add(this.maze[currX][currY + distance]);
        }
        // down
        if (validPoint(currX, currY - distance) && !visited.contains(this.maze[currX][currY - distance])) {
            nonVisitedAdjacentPoints.add(this.maze[currX][currY - distance]);
        }

        final int min = 0;
        final int max = nonVisitedAdjacentPoints.size() - 1;
        int randomInt = (int) (Math.random() * (max - min + 1));

        return nonVisitedAdjacentPoints.isEmpty() ? null : nonVisitedAdjacentPoints.get(randomInt);
    }

    /**
     * If (x,y) is within the 4 outer walls of the maze.
     */
    private boolean validPoint(int x, int y) {
        final int firstColumn = 1;
        final int lastColumn = this.columns - 2;

        final int firstRow = 1;
        final int lastRow = this.rows - 2;

        return x >= firstColumn && x <= lastColumn
                && y >= firstRow && y <= lastRow;
    }

    private void removeWallBetweenPoints(MazeObject pointA, MazeObject pointB) {
        if (pointA.getRow() + 2 == pointB.getRow()) {
            this.maze[pointA.getCol()][pointA.getRow() + 1].setPassable(true);

        } else if (pointA.getRow() - 2 == pointB.getRow()) {
            this.maze[pointA.getCol()][pointA.getRow() - 1].setPassable(true);

        } else if (pointA.getCol() + 2 == pointB.getCol()) {
            this.maze[pointA.getCol() + 1][pointA.getRow()].setPassable(true);

        } else {
            this.maze[pointA.getCol() - 1][pointA.getRow()].setPassable(true);
        }
    }

}