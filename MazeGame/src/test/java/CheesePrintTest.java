import game.Cheese;
import game.MazeBuilder;
import game.MazeObject;
import game.Player;

public class CheesePrintTest {
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
        int numRow = 4;
        int numCol = 4;
        MazeBuilder builder = new MazeBuilder(numRow, numCol);
        MazeObject[][] maze = builder.getMaze();

        // set all to cheese
        maze[1][1] = new Player(6, 1, 1);
        maze[1][2] = new Cheese(2, 1);
        maze[2][1] = new Cheese(1, 2);


        printMaze(maze, numRow, numCol);
        System.out.println("Num rows: " + maze.length);
        System.out.println("Num cols: " + maze[0].length);

    }
}
