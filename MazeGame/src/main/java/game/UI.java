package game;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UI extends Application {

    // Tile size in pixels
    private static final int TILE_SIZE = 32;
    private static final double PLAYER_HEIGHT = 32 * 1.5;
    private static final double PLAYER_HEIGHT_OFFSET = TILE_SIZE - PLAYER_HEIGHT;

    // Placeholder images
    private Image imgPlayer;
    private Image imgWall;
    private Image imgFloor;
    private Image imgCheese;
    private Image imgDark;
    private Client client = new Client();

    // Maze instance
    private Maze maze;


    @Override
    public void start(Stage primaryStage) {
        // Menu Screen
        Button startButton = new Button("Start Game");
        Button howToPlayButton = new Button("How To Play");

        VBox menuLayout = new VBox(20, startButton, howToPlayButton);
        menuLayout.setAlignment(Pos.CENTER);
        Scene menuScene = new Scene(menuLayout, 400, 300);
//        try {
//            maze = client.setupConnection();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // Create the Maze object (generates the 2D array)
         maze = new Maze();
         maze.placeCheeseRandomly();

        // Load images (make sure these are in src/main/resources/game/)
        imgPlayer = loadImage("1-1.png");
        imgWall   = loadImage("wall_placeholder.png");
        imgFloor  = loadImage("floor_placeholder.png");
        imgCheese = loadImage("Cheese.png");
        imgDark = loadImage("dark_placeholder.png");


        // Get maze size from Maze class
        MazeObject[][] grid = maze.getMaze();
        int rows = grid.length;
        int cols = grid[0].length;

        // Canvas size based on maze dimensions
        Canvas canvas = new Canvas(cols * TILE_SIZE, rows * TILE_SIZE);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Draw the board once
        drawBoard(gc);

        // Setup scene and stage
        StackPane gameRoot = new StackPane(canvas);
        Scene gameScene = new Scene(gameRoot);

        gameScene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case W -> maze.moveWithUserInput('w');
                case A -> maze.moveWithUserInput('a');
                case S -> maze.moveWithUserInput('s');
                case D -> maze.moveWithUserInput('d');
                default -> {
                    // ignore other keys
                }
            }
            // redraw the board after the move
            drawBoard(gc);
        });

//        primaryStage.setScene(scene);
//        primaryStage.setTitle("Maze Game");
//        primaryStage.show();


        // Button actions
        startButton.setOnAction(e -> {
            primaryStage.setScene(gameScene);
            drawBoard(gc);  // Draw initial board when starting
            canvas.requestFocus();           // Make sure key events go to canvas
        });

        howToPlayButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("How To Play");
            alert.setHeaderText("Instructions");
            alert.setContentText("Use WASD keys to move your player around the maze and collect cheese.");
            alert.showAndWait();
        });

        // Show menu scene first
        primaryStage.setScene(menuScene);
        primaryStage.setTitle("Maze Game");
        primaryStage.show();
    }

    private void drawBoard(GraphicsContext gc) {
        MazeObject[][] grid = maze.getMaze();

        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                MazeObject obj = grid[row][col];

                if (!obj.isVisible()) {
                    drawImage(gc, imgDark, row, col);
                } else if (!obj.isPassable()) {
                    drawImage(gc, imgWall, row, col);
                } else {
                    drawImage(gc, imgFloor, row, col);
                }
            }
        }

        // Cheese is always drawn, regardless of visibility
        Cheese cheese = maze.getCheese();
        int cheeseRow = cheese.getRow();
        int cheeseCol = cheese.getCol();
        drawImage(gc, imgCheese, cheeseRow, cheeseCol);


        // Player is also always drawn
        for (Player p : maze.getPlayers()) {
            if (p != null) {
                drawImage(gc, imgPlayer, p.getRow(), p.getCol(),
                        TILE_SIZE, PLAYER_HEIGHT, PLAYER_HEIGHT_OFFSET);
            }
        }
    }

    // Default size
    private void drawImage(GraphicsContext gc, Image img, int row, int col) {
        drawImage(gc, img, row, col, TILE_SIZE, TILE_SIZE, 0);
    }

    // Custom size
    private void drawImage(GraphicsContext gc, Image img, int row, int col,
                           double width, double height, double yOffset) {

        gc.drawImage(img, col * TILE_SIZE, row * TILE_SIZE + yOffset, width, height);
    }


    private Image loadImage(String filename) {
        return new Image(getClass().getResourceAsStream("/game/" + filename));
    }



    public static void main(String[] args) {
        launch(args);
    }
}
