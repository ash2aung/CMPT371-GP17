package game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class UI extends Application {

    // Tile size in pixels (easy to tweak later)
    private static final int TILE_SIZE = 32;

    // Placeholder images
    private Image imgPlayer;
    private Image imgWall;
    private Image imgFloor;
    private Image imgCheese;
    private Client client;

    // Maze instance
    private Maze maze;

    @Override
    public void start(Stage primaryStage) {
        maze = client.setupConnection();

        // Create the Maze object (generates the 2D array)
        maze = new Maze();
        maze.placeCheeseRandomly();

        // Load images (make sure these are in src/main/resources/game/)
        imgPlayer = new Image(getClass().getResourceAsStream("/game/player_placeholder.png"));
        imgWall   = new Image(getClass().getResourceAsStream("/game/wall_placeholder.png"));
        imgFloor  = new Image(getClass().getResourceAsStream("/game/floor_placeholder.png"));
        imgCheese = new Image(getClass().getResourceAsStream("/game/cheese_placeholder.png"));

        // Get maze size from Maze class
        MazeObject[][] grid = maze.getMaze();
        int rows = grid.length;
        int cols = grid[0].length;

        // Canvas size based on maze dimensions
        Canvas canvas = new Canvas(cols * TILE_SIZE, rows * TILE_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Draw the board once
        drawBoard(gc, grid);

        // Setup scene and stage
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

        scene.setOnKeyPressed(event -> {
            // TODO: Only change since Monday is this now calls the functions that update the client
            switch (event.getCode()) {
                case W -> maze.moveWithUserInput('w', client);
                case A -> maze.moveWithUserInput('a', client);
                case S -> maze.moveWithUserInput('s', client);
                case D -> maze.moveWithUserInput('d', client);
                default -> {
                    // ignore other keys
                }
            }
            // redraw the board after the move
            drawBoard(gc, maze.getMaze());
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Maze Game");
        primaryStage.show();



    }

    private void drawBoard(GraphicsContext gc, MazeObject[][] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                MazeObject obj = grid[row][col];

                if (obj instanceof Player) {
                    gc.drawImage(imgPlayer, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (obj instanceof Cheese) {
                    gc.drawImage(imgCheese, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else if (!obj.isPassable()) {
                    gc.drawImage(imgWall, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else {
                    gc.drawImage(imgFloor, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
