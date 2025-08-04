import game.*;

import java.util.Scanner;

public class TestRunConsoleGame {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        Maze maze = new Maze();

        maze.placeCheeseRandomly();
        maze.revealEntireMaze();


        while (true) {
            // row, col inputs
            System.out.print("Id: ");
            int id = input.nextInt();

            System.out.printf("Row: ");
            int row = input.nextInt();

            System.out.printf("Col: ");
            int col = input.nextInt();

            maze.processPlayerMove(id, row, col);


            // key inputs
//            System.out.print("Key: ");
//            String tempInput = input.nextLine();
//            char key = tempInput.charAt(0);
//            maze.moveWithUserInput(key);
        }
    }
}
