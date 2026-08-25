package games.mrlaki5.backgammon.Online.anticheat;

import com.royalbackgammon.core.logic.BackgammonRules;
import com.royalbackgammon.core.logic.MoveExecutor;
import com.royalbackgammon.core.model.Die;
import com.royalbackgammon.core.model.GameState;
import com.royalbackgammon.core.model.Move;
import com.royalbackgammon.core.model.Player;

import java.util.List;

import games.mrlaki5.backgammon.Online.sync.GameEvent;

/**
 * Client-side anti-cheat validation using the game-core rules engine.
 *
 * Validates incoming moves from the opponent before applying them to local state.
 * This ensures:
 * 1. The opponent is not making illegal moves
 * 2. Moves are consistent with the current board state
 * 3. Dice values match what was rolled
 *
 * Note: Full anti-cheat requires SERVER-SIDE validation as well.
 * The server should run the same game-core logic to verify every event
 * before broadcasting it. This class provides the client-side check
 * for immediate detection and UI responsiveness.
 */
public class MoveValidator {

    private GameState gameState;

    public MoveValidator(GameState initialState) {
        this.gameState = initialState;
    }

    /**
     * Updates the tracked game state (e.g., after a reconnect with fresh state).
     */
    public void updateState(GameState newState) {
        this.gameState = newState;
    }

    /**
     * Validates a MOVE event from the opponent.
     * Returns a ValidationResult indicating if the move is legal.
     */
    public ValidationResult validateMove(GameEvent event) {
        if (event.getType() != GameEvent.Type.MOVE) {
            return ValidationResult.invalid("Event is not a MOVE type");
        }

        // Check it's the correct player's turn
        if (event.getPlayer() != gameState.getCurrentPlayer()) {
            return ValidationResult.invalid("Not this player's turn. Expected player "
                    + gameState.getCurrentPlayer() + ", got " + event.getPlayer());
        }

        // Calculate legal moves for current state
        List<Move> legalMoves = BackgammonRules.calculateLegalMoves(
                gameState.getBoard(), gameState.getCurrentPlayer(), gameState.getDice());

        // Check if the move is in the legal moves list
        Move matchingMove = BackgammonRules.isLegalMove(
                event.getFrom(), event.getTo(), legalMoves);

        if (matchingMove == null) {
            return ValidationResult.invalid("Move from " + event.getFrom()
                    + " to " + event.getTo() + " is not legal in current state");
        }

        // Check die value matches
        if (matchingMove.getDieValue() != event.getDieValue()) {
            return ValidationResult.invalid("Die value mismatch. Expected "
                    + matchingMove.getDieValue() + ", got " + event.getDieValue());
        }

        return ValidationResult.valid();
    }

    /**
     * Validates a DICE_ROLL event.
     * Client-side can only check format — the actual values are server-authoritative.
     */
    public ValidationResult validateDiceRoll(GameEvent event) {
        if (event.getType() != GameEvent.Type.DICE_ROLL) {
            return ValidationResult.invalid("Event is not a DICE_ROLL type");
        }

        int[] values = event.getDiceValues();
        if (values == null || (values.length != 2 && values.length != 4)) {
            return ValidationResult.invalid("Invalid dice values array");
        }

        for (int v : values) {
            if (v < 0 || v > 6) {
                return ValidationResult.invalid("Dice value out of range: " + v);
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Applies a validated event to the local game state.
     * Call only after validation passes.
     */
    public void applyEvent(GameEvent event) {
        switch (event.getType()) {
            case MOVE:
                Move move = new Move(event.getDieValue(), event.getFrom(), event.getTo());
                MoveExecutor.applyMove(gameState, move);
                break;
            case DICE_ROLL:
                int[] values = event.getDiceValues();
                if (values.length >= 2) {
                    gameState.getDice()[0] = new Die(values[0], false);
                    gameState.getDice()[1] = new Die(values[1], false);
                    if (values.length == 4) {
                        gameState.getDice()[2] = new Die(values[2], false);
                        gameState.getDice()[3] = new Die(values[3], false);
                    } else {
                        gameState.getDice()[2] = new Die(0, true);
                        gameState.getDice()[3] = new Die(0, true);
                    }
                }
                gameState.setTurnState(GameState.STATE_MOVE);
                break;
            case NO_MOVES:
                gameState.switchPlayer();
                gameState.setTurnState(GameState.STATE_ROLL);
                break;
            case GAME_OVER:
                gameState.setWinner(event.getWinner());
                break;
            default:
                break;
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    /**
     * Result of a move validation check.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
    }
}
