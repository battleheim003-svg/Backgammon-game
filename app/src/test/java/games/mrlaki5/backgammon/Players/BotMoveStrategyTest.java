package games.mrlaki5.backgammon.Players;

import org.junit.Test;

import java.util.List;
import java.util.Random;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.DiceThrow;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.GameControllers.GameLogic;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GamePreferences;

import static org.junit.Assert.assertEquals;

public class BotMoveStrategyTest {

    @Test
    public void hardBotPrefersHittingBlot() {
        Model model = modelWithEmptyBoard(1, dice(3, 5));
        GameLogic logic = new GameLogic(model);
        board(model)[11] = field(1, 1);
        board(model)[10] = field(2, 1);
        board(model)[8] = field(1, 2);

        List<NextJump> moves = logic.calculateMoves(board(model), 1, model.getDiceThrows());
        NextJump chosen = new BotMoveStrategy().chooseMove(model, moves,
                GamePreferences.BOT_HARD, new Random(4));

        assertEquals(11, chosen.getSrcField());
        assertEquals(8, chosen.getDstField());
    }

    @Test
    public void royalBotPrefersBearingOffInHomeBoard() {
        Model model = modelWithEmptyBoard(1, dice(1, 2));
        GameLogic logic = new GameLogic(model);
        board(model)[23] = field(1, 1);
        board(model)[22] = field(2, 1);

        List<NextJump> moves = logic.calculateMoves(board(model), 1, model.getDiceThrows());
        NextJump chosen = new BotMoveStrategy().chooseMove(model, moves,
                GamePreferences.BOT_ROYAL, new Random(2));

        assertEquals(27, chosen.getDstField());
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
        dice[2] = new DiceThrow(0, 1);
        dice[3] = new DiceThrow(0, 1);
        return dice;
    }
}
