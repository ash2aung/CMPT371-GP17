import game.*;


public class TestRowsAndCols {
    public static void printMaze(MazeObject[][] maze, int nr, int nc) {

        for (int row = 0; row < nr; row++) {
            for (int col = 0; col < nc; col++) {
                printMazeObject(maze[row][col]);
            }
            System.out.println();
        }


    }

    public static void printMazeObject(MazeObject obj) {
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

    public static void main(String[] args) {
        int numRow = 5;
        int numCol = 10;
        MazeBuilder builder = new MazeBuilder(numRow, numCol);
        MazeObject[][] maze = builder.getMaze();


        printMaze(maze, numRow, numCol);
        System.out.println("Num rows: " + maze.length);
        System.out.println("Num cols: " + maze[0].length);

    }


}
