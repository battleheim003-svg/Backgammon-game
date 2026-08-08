package games.mrlaki5.backgammon.GameControllers;

import java.util.List;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.DiceThrow;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.GameModel.Model;

// Applies checker moves to the model without depending on Android UI classes.
public class GameMoveExecutor {
    private final Model model;

    public static class MoveResult {
        private final boolean applied;
        private final int sourceField;
        private final int destinationField;
        private final boolean hit;

        private MoveResult(boolean applied, int sourceField, int destinationField, boolean hit) {
            this.applied = applied;
            this.sourceField = sourceField;
            this.destinationField = destinationField;
            this.hit = hit;
        }

        public boolean isApplied() {
            return applied;
        }

        public int getSourceField() {
            return sourceField;
        }

        public int getDestinationField() {
            return destinationField;
        }

        public boolean isHit() {
            return hit;
        }
    }

    public GameMoveExecutor(Model model) {
        this.model = model;
    }

    public boolean applyPickedUpMove(int srcField, int dstField, List<NextJump> legalMoves) {
        return applyPickedUpMoveWithResult(srcField, dstField, legalMoves).isApplied();
    }

    public MoveResult applyPickedUpMoveWithResult(int srcField, int dstField,
                                                  List<NextJump> legalMoves) {
        NextJump jump = findMove(srcField, dstField, legalMoves);
        if (jump == null) {
            restorePickedUpChecker(srcField);
            return new MoveResult(false, srcField, dstField, false);
        }

        consumeDice(jump.getJumpNumber());
        boolean hit = placeChecker(dstField);
        return new MoveResult(true, srcField, dstField, hit);
    }

    public MoveResult applyMove(NextJump jump) {
        BoardFieldState[] board = model.getBoardFields();
        int src = jump.getSrcField();

        board[src].setNumberOfChips(board[src].getNumberOfChips() - 1);
        if (board[src].getNumberOfChips() == 0) {
            board[src].setPlayer(0);
        }

        consumeDice(jump.getJumpNumber());
        boolean hit = placeChecker(jump.getDstField());
        return new MoveResult(true, src, jump.getDstField(), hit);
    }

    private NextJump findMove(int srcField, int dstField, List<NextJump> legalMoves) {
        for (NextJump jump : legalMoves) {
            if (jump.getSrcField() == srcField && jump.getDstField() == dstField) {
                return jump;
            }
        }
        return null;
    }

    private void restorePickedUpChecker(int srcField) {
        BoardFieldState[] board = model.getBoardFields();
        board[srcField].setNumberOfChips(board[srcField].getNumberOfChips() + 1);
        if (board[srcField].getNumberOfChips() == 1) {
            board[srcField].setPlayer(model.getCurrentPlayer());
        }
    }

    private void consumeDice(int throwNumber) {
        for (DiceThrow dice : model.getDiceThrows()) {
            if (dice.getThrowNumber() == throwNumber && dice.getAlreadyUsed() == 0) {
                dice.setAlreadyUsed(1);
                return;
            }
        }
    }

    private boolean placeChecker(int dstField) {
        BoardFieldState[] board = model.getBoardFields();
        int player = model.getCurrentPlayer();
        int destinationPlayer = board[dstField].getPlayer();
        boolean hit = board[dstField].getNumberOfChips() == 1 && destinationPlayer != player;

        if (hit) {
            int bar = 23 + destinationPlayer;
            board[bar].setNumberOfChips(board[bar].getNumberOfChips() + 1);
            board[bar].setPlayer(destinationPlayer);
        } else {
            board[dstField].setNumberOfChips(board[dstField].getNumberOfChips() + 1);
        }

        if (board[dstField].getNumberOfChips() == 1) {
            board[dstField].setPlayer(player);
        }
        return hit;
    }
}
