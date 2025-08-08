package game;

public interface ClientEventListener {
    void onMoveReceived(int playerID, int row, int col);
    void onCheeseReceived(int row, int col);
}
