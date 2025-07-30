import game.*;

import java.util.Scanner;

public class TestRun {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        Maze maze = new Maze();

//        maze.revealEntireMaze();

        while (true) {
//            System.out.print("Id: ");
//            int id = input.nextInt();
//
//            System.out.printf("Row: ");
//            int row = input.nextInt();
//
//            System.out.printf("Col: ");
//            int col = input.nextInt();
//
//            PlayerMove move = new PlayerMove(id, row, col);
//            maze.movePlayer(id, row, col);
            System.out.print("Key: ");
            String tempInput = input.nextLine();
            char key = tempInput.charAt(0);
            maze.moveWithUserInput(key);
        }
    }
}
