import game.*;

import java.util.Scanner;

public class TestRunConsoleGame {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        Maze maze = new Maze();

        maze.placeCheeseRandomly();
        maze.revealEntireMaze();

        maze.findCheese();

        System.out.print("Row: ");
        int row = input.nextInt();
        System.out.print("Col: ");
        int col = input.nextInt();

        MazeObject[][] tempMaze = maze.getMaze();;
        MazeObject obj = tempMaze[row][col];
        if (obj instanceof Cheese) {
            System.out.println("WTF IS GOING ON!");
        }


//        while (true) {
            // row, col inputs
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


            // key inputs
//            System.out.print("Key: ");
//            String tempInput = input.nextLine();
//            char key = tempInput.charAt(0);
//            maze.moveWithUserInput(key);
//        }
    }
}
