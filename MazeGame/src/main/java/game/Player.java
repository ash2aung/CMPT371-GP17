package game;

public class Player extends MazeObject{
    private int cheeseAmount;
    private int playerId;

    public Player(int playerId, int col, int row) {
        super(col, row, false, true);
        this.playerId = playerId;
        this.cheeseAmount = 0;
    }

    public int getCheeseAmount() {
        return cheeseAmount;
    }

    public void addCheeseCollected() {
        cheeseAmount++;
    }

    public int getId() {
        return playerId;
    }

    @Override
    public String toString() {
        return "" + playerId;
    }
}
