package games.mrlaki5.backgammon.GameControllers;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.DiceThrow;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.GameModel.Model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameLogicTest {

    @Test
    public void calculateMoves_movesCheckerByUnusedDice() {
        Model model = modelWithEmptyBoard(1, dice(3, 5));
        board(model)[11] = field(1, 1);
        GameLogic logic = new GameLogic(model);

        List<NextJump> moves = logic.calculateMoves(board(model), 1, model.getDiceThrows());

        assertTrue(hasMove(moves, 11, logic.calculateMatrixPosition(4, 1), 3));
        assertTrue(hasMove(moves, 11, logic.calculateMatrixPosition(6, 1), 5));
    }

    @Test
    public void calculateMoves_requiresBarEntryBeforeOtherMoves() {
        Model model = modelWithEmptyBoard(1, dice(1, 2));
        board(model)[24] = field(1, 1);
        board(model)[11] = field(1, 1);
        GameLogic logic = new GameLogic(model);

        List<NextJump> moves = logic.calculateMoves(board(model), 1, model.getDiceThrows());

        assertTrue(hasMove(moves, 24, logic.calculateMatrixPosition(1, 1), 1));
        assertTrue(hasMove(moves, 24, logic.calculateMatrixPosition(2, 1), 2));
        assertFalse(hasSource(moves, 11));
    }

    @Test
    public void calculateMoves_allowsBearingOffFromHomeBoard() {
        Model model = modelWithEmptyBoard(1, dice(1, 2));
        board(model)[23] = field(1, 1);
        GameLogic logic = new GameLogic(model);

        List<NextJump> moves = logic.calculateMoves(board(model), 1, model.getDiceThrows());

        assertTrue(hasMove(moves, 23, 27, 1));
        assertTrue(hasMove(moves, 23, 27, 2));
    }

    @Test
    public void moveExecutor_hitsOpponentBlotAndConsumesDice() {
        Model model = modelWithEmptyBoard(1, dice(3, 5));
        board(model)[11] = field(1, 1);
        board(model)[14] = field(1, 2);
        GameMoveExecutor executor = new GameMoveExecutor(model);

        executor.applyMove(new NextJump(3, 11, 14));

        assertEquals(0, board(model)[11].getNumberOfChips());
        assertEquals(0, board(model)[11].getPlayer());
        assertEquals(1, board(model)[14].getNumberOfChips());
        assertEquals(1, board(model)[14].getPlayer());
        assertEquals(1, board(model)[25].getNumberOfChips());
        assertEquals(2, board(model)[25].getPlayer());
        assertEquals(1, model.getDiceThrows()[0].getAlreadyUsed());
    }

    @Test
    public void moveExecutor_restoresPickedUpCheckerWhenDestinationIsIllegal() {
        Model model = modelWithEmptyBoard(1, dice(3, 5));
        board(model)[11] = field(0, 0);
        GameMoveExecutor executor = new GameMoveExecutor(model);

        boolean applied = executor.applyPickedUpMove(11, 12,
                Arrays.asList(new NextJump(3, 11, 14)));

        assertFalse(applied);
        assertEquals(1, board(model)[11].getNumberOfChips());
        assertEquals(1, board(model)[11].getPlayer());
    }

    private static Model modelWithEmptyBoard(int currentPlayer, DiceThrow[] diceThrows) {
        Model model = new Model();
        BoardFieldState[] board = new BoardFieldState[28];
        for (int i = 0; i < board.length; i++) {
            board[i] = field(0, 0);
        }
        model.setBoardFields(board);
        model.setCurrentPlayer(currentPlayer);
        model.setDiceThrows(diceThrows);
        return model;
    }

    private static BoardFieldState[] board(Model model) {
        return model.getBoardFields();
    }

    private static BoardFieldState field(int chips, int player) {
        return new BoardFieldState(chips, player);
    }

    private static DiceThrow[] dice(int first, int second) {
        DiceThrow[] dice = new DiceThrow[4];
        dice[0] = new DiceThrow(first);
        dice[1] = new DiceThrow(second);
        dice[2] = new DiceThrow(0);
        dice[3] = new DiceThrow(0);
        dice[2].setAlreadyUsed(1);
        dice[3].setAlreadyUsed(1);
        return dice;
    }

    private static boolean hasMove(List<NextJump> moves, int src, int dst, int jump) {
        for (NextJump move : moves) {
            if (move.getSrcField() == src && move.getDstField() == dst
                    && move.getJumpNumber() == jump) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSource(List<NextJump> moves, int src) {
        for (NextJump move : moves) {
            if (move.getSrcField() == src) {
                return true;
            }
        }
        return false;
    }
}
