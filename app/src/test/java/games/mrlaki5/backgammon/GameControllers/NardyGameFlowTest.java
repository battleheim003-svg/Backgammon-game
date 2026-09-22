package games.mrlaki5.backgammon.GameControllers;

import com.royalbackgammon.core.logic.BackgammonRules;
import com.royalbackgammon.core.model.BoardField;
import com.royalbackgammon.core.model.GameResult;
import com.royalbackgammon.core.model.GameState;
import com.royalbackgammon.core.variant.Variant;

import org.junit.Test;

import java.util.List;
import java.util.Random;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.Players.BotMoveStrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Bot-vs-bot games through the app's Model / GameLogic / GameMoveExecutor stack. */
public class NardyGameFlowTest {

    @Test
    public void botGamesFinishUnderNardyRules() {
        for (int seed = 0; seed < 3; seed++) {
            playBotGame(Variant.NARDY, seed);
        }
    }

    @Test
    public void botGameStillFinishesUnderStandardRules() {
        playBotGame(Variant.STANDARD, 11);
    }

    @Test
    public void undoResetRestoresHeadMove() {
        Model model = newModel(Variant.NARDY);
        GameLogic logic = new GameLogic(model);
        model.setDiceThrows(logic.rollDices());
        model.setState(2);
        List<NextJump> moves = logic.calculateMoves(model.getBoardFields(), 1, model.getDiceThrows());
        new GameMoveExecutor(model).applyMove(moves.get(0));
        assertEquals(1, model.getHeadMovesThisTurn());
        model.onTurnEnded();
        assertEquals(0, model.getHeadMovesThisTurn());
        assertEquals(1, model.getTurnsPlayed());
    }

    private void playBotGame(Variant variant, int seed) {
        Random random = new Random(seed);
        Model model = newModel(variant);
        GameLogic logic = new GameLogic(model);
        GameMoveExecutor executor = new GameMoveExecutor(model);
        BotMoveStrategy bot = new BotMoveStrategy();
        int head1 = BackgammonRules.headIndex(1, variant);
        int head2 = BackgammonRules.headIndex(2, variant);

        for (int turn = 0; turn < 600 && logic.getCurrPlayerFinished() == 0; turn++) {
            model.setState(3);
            model.setDiceThrows(logic.rollDices());
            model.setState(2);
            int player = model.getCurrentPlayer();
            int headMoves = 0;
            while (true) {
                List<NextJump> moves = logic.calculateMoves(model.getBoardFields(), player,
                        model.getDiceThrows());
                if (logic.getCurrPlayerFinished() != 0 || moves.isEmpty()) break;
                NextJump move = bot.chooseMove(model, moves, GamePreferences.BOT_MEDIUM, random);
                if (variant == Variant.NARDY) {
                    BoardFieldState dst = model.getBoardFields()[move.getDstField()];
                    assertTrue(move.getDstField() >= 24 || dst.getNumberOfChips() == 0
                            || dst.getPlayer() == player);
                    if (move.getSrcField() == (player == 1 ? head1 : head2)) headMoves++;
                }
                executor.applyMove(move);
                assertEquals(30, totalCheckers(model));
            }
            if (variant == Variant.NARDY && model.getTurnsPlayed() >= 2) {
                assertTrue("head rule", headMoves <= 1);
            }
            if (logic.getCurrPlayerFinished() != 0) break;
            model.onTurnEnded();
            model.changeCurrentPlayer();
        }

        GameResult result = logic.calculateResult();
        assertNotNull("game did not finish (" + variant + ", seed " + seed + ")", result);
        assertEquals(logic.getCurrPlayerFinished(), result.getWinner());
    }

    private static Model newModel(Variant variant) {
        Model model = new Model();
        model.setVariant(variant);
        BoardField[] start = GameState.newGame(variant).getBoard();
        BoardFieldState[] board = new BoardFieldState[28];
        for (int i = 0; i < board.length; i++) {
            board[i] = new BoardFieldState(start[i].getChipCount(), start[i].getOwner());
        }
        model.setBoardFields(board);
        model.setCurrentPlayer(1);
        model.setState(3);
        return model;
    }

    private static int totalCheckers(Model model) {
        int total = 0;
        for (BoardFieldState f : model.getBoardFields()) total += f.getNumberOfChips();
        return total;
    }
}
