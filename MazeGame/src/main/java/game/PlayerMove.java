package game;

public class PlayerMove {
    private int playerId;
    private int row;
    private int col;


    public PlayerMove(int playerId, int row, int col) {
        this.playerId = playerId;
        this.row = row;
        this.col = col;

    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getPlayerId() {
        return playerId;
    }
}
