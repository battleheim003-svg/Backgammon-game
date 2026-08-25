package games.mrlaki5.backgammon.Online.sync;

/**
 * A lightweight event representing a single game action.
 * Sent between clients to synchronize game state.
 *
 * Design: events are small (not full state) to minimize data usage —
 * important for users with limited/expensive mobile data in Iran.
 *
 * JSON structure when serialized:
 * {
 *   "matchId": "...",
 *   "turn": 12,
 *   "type": "MOVE",
 *   "player": 1,
 *   "from": 6,
 *   "to": 3,
 *   "dieValue": 3,
 *   "timestamp": 1692000000000
 * }
 */
public class GameEvent {

    public enum Type {
        /** A checker move from one point to another. */
        MOVE,
        /** Dice roll result. */
        DICE_ROLL,
        /** Player has no legal moves — turn is forfeited. */
        NO_MOVES,
        /** Player resigned/left the game. */
        RESIGN,
        /** Player reconnected after a disconnect. */
        RECONNECT,
        /** Game is over — contains winner info. */
        GAME_OVER
    }

    private String matchId;
    private int turn;
    private Type type;
    private int player;
    private int from;
    private int to;
    private int dieValue;
    private int[] diceValues; // For DICE_ROLL events (2 or 4 values)
    private int winner;       // For GAME_OVER events
    private long timestamp;

    public GameEvent() {}

    // --- Factory methods ---

    public static GameEvent move(String matchId, int turn, int player,
                                  int from, int to, int dieValue) {
        GameEvent e = new GameEvent();
        e.matchId = matchId;
        e.turn = turn;
        e.type = Type.MOVE;
        e.player = player;
        e.from = from;
        e.to = to;
        e.dieValue = dieValue;
        e.timestamp = System.currentTimeMillis();
        return e;
    }

    public static GameEvent diceRoll(String matchId, int turn, int player, int[] values) {
        GameEvent e = new GameEvent();
        e.matchId = matchId;
        e.turn = turn;
        e.type = Type.DICE_ROLL;
        e.player = player;
        e.diceValues = values;
        e.timestamp = System.currentTimeMillis();
        return e;
    }

    public static GameEvent noMoves(String matchId, int turn, int player) {
        GameEvent e = new GameEvent();
        e.matchId = matchId;
        e.turn = turn;
        e.type = Type.NO_MOVES;
        e.player = player;
        e.timestamp = System.currentTimeMillis();
        return e;
    }

    public static GameEvent resign(String matchId, int turn, int player) {
        GameEvent e = new GameEvent();
        e.matchId = matchId;
        e.turn = turn;
        e.type = Type.RESIGN;
        e.player = player;
        e.timestamp = System.currentTimeMillis();
        return e;
    }

    public static GameEvent reconnect(String matchId, int turn, int player) {
        GameEvent e = new GameEvent();
        e.matchId = matchId;
        e.turn = turn;
        e.type = Type.RECONNECT;
        e.player = player;
        e.timestamp = System.currentTimeMillis();
        return e;
    }

    public static GameEvent gameOver(String matchId, int turn, int winner) {
        GameEvent e = new GameEvent();
        e.matchId = matchId;
        e.turn = turn;
        e.type = Type.GAME_OVER;
        e.winner = winner;
        e.timestamp = System.currentTimeMillis();
        return e;
    }

    // --- Getters ---

    public String getMatchId() { return matchId; }
    public int getTurn() { return turn; }
    public Type getType() { return type; }
    public int getPlayer() { return player; }
    public int getFrom() { return from; }
    public int getTo() { return to; }
    public int getDieValue() { return dieValue; }
    public int[] getDiceValues() { return diceValues; }
    public int getWinner() { return winner; }
    public long getTimestamp() { return timestamp; }

    // --- Setters (for deserialization) ---

    public void setMatchId(String matchId) { this.matchId = matchId; }
    public void setTurn(int turn) { this.turn = turn; }
    public void setType(Type type) { this.type = type; }
    public void setPlayer(int player) { this.player = player; }
    public void setFrom(int from) { this.from = from; }
    public void setTo(int to) { this.to = to; }
    public void setDieValue(int dieValue) { this.dieValue = dieValue; }
    public void setDiceValues(int[] diceValues) { this.diceValues = diceValues; }
    public void setWinner(int winner) { this.winner = winner; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
