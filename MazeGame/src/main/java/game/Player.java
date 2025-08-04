package game;

public class Player extends MazeObject{
    private int cheeseCount;
    private int playerId;

    public Player(int playerId, int col, int row) {
        super(col, row, false, true);
        this.playerId = playerId;
        this.cheeseCount = 0;
    }

    public int getCheeseCount() {
        return cheeseCount;
    }

    public void addCheeseCount() {
        cheeseCount++;
    }

    public int getId() {
        return playerId;
    }

    @Override
    public String toString() {
        return "" + playerId;
    }
}
