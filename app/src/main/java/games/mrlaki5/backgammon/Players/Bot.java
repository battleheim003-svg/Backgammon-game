package games.mrlaki5.backgammon.Players;

import java.util.List;
import java.util.Random;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.GameControllers.GameActivity;
import games.mrlaki5.backgammon.GameControllers.GameLogic;
import games.mrlaki5.backgammon.GameControllers.GameMoveExecutor;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GamePreferences;
import games.mrlaki5.backgammon.GameView.OnBoardImage;

/** Bot with four genuinely different move-selection profiles. */
public class Bot extends Player {
    private final Model model;
    private final GameMoveExecutor moveExecutor;
    private final Random random = new Random();

    public Bot(GameActivity currGame, String playerName, Model model) {
        super(currGame, playerName);
        this.model = model;
        this.moveExecutor = new GameMoveExecutor(model);
    }

    private int difficulty() {
        return GamePreferences.getBotDifficulty(getCurrGame());
    }

    private long thinkTime() {
        switch (difficulty()) {
            case GamePreferences.BOT_EASY: return 600L;
            case GamePreferences.BOT_HARD: return 330L;
            case GamePreferences.BOT_ROYAL: return 260L;
            default: return 430L;
        }
    }

    @Override
    public synchronized void actionMove() {
        OnBoardImage boardImage = getCurrGame().getBoardImage();
        GameLogic gameLogic = getCurrGame().getGameLogic();
        setWaitCond(1);

        while (!model.getNextMoves().isEmpty()) {
            try {
                wait(thinkTime());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            if (getWaitCond() == 0) return;

            NextJump jump = chooseMove(model.getNextMoves(), gameLogic);
            applyMove(jump, boardImage);
            boardImage.postInvalidate();
            model.setNextMoves(gameLogic.calculateMoves(
                    model.getBoardFields(), model.getCurrentPlayer(), model.getDiceThrows()));
        }
    }

    private NextJump chooseMove(List<NextJump> moves, GameLogic logic) {
        if (difficulty() == GamePreferences.BOT_EASY) {
            return moves.get(random.nextInt(moves.size()));
        }

        NextJump best = moves.get(0);
        double bestScore = -Double.MAX_VALUE;
        for (NextJump move : moves) {
            double score = scoreMove(move, logic, difficulty());
            // Small variation prevents the bot from repeating the same game forever.
            score += random.nextDouble() * (difficulty() == GamePreferences.BOT_MEDIUM ? 18 : 4);
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    private double scoreMove(NextJump move, GameLogic logic, int level) {
        BoardFieldState[] board = model.getBoardFields();
        int player = model.getCurrentPlayer();
        int opponent = player == 1 ? 2 : 1;
        int src = move.getSrcField();
        int dst = move.getDstField();
        int srcCount = board[src].getNumberOfChips();
        int dstCount = board[dst].getNumberOfChips();
        int dstPlayer = board[dst].getPlayer();

        double score = move.getJumpNumber() * 2.0;
        if (dst == 26 || dst == 27) score += level == GamePreferences.BOT_ROYAL ? 150 : 95;
        if (dstPlayer == opponent && dstCount == 1) score += level >= GamePreferences.BOT_HARD ? 115 : 70;
        if (dstPlayer == player && dstCount == 1) score += level >= GamePreferences.BOT_HARD ? 65 : 35;
        if (dstPlayer == player && dstCount >= 4) score -= level >= GamePreferences.BOT_HARD ? 26 : 8;
        if (srcCount == 2) score -= level >= GamePreferences.BOT_HARD ? 52 : 18;
        if (srcCount == 1) score += 20; // rescue a blot

        int realSrc = logic.calculateRealPosition(src, player);
        int realDst = (dst == 26 || dst == 27) ? 25 : logic.calculateRealPosition(dst, player);
        score += Math.max(0, realDst - realSrc) * (level == GamePreferences.BOT_ROYAL ? 2.2 : 1.2);

        if (level == GamePreferences.BOT_ROYAL) {
            // Royal bot values prime-building and safe home-board points.
            if (realDst >= 19 && realDst <= 24 && dstPlayer == player) score += 35;
            if (dstCount == 0) score -= 10;
            if ((player == 1 && src == 24) || (player == 2 && src == 25)) score += 80;
        }
        return score;
    }

    private void applyMove(NextJump jump, OnBoardImage boardImage) {
        moveExecutor.applyMove(jump);
        boardImage.setDices(model.getDiceThrows());
    }

    @Override
    public synchronized void actionRoll() {
        setWaitCond(1);
        getCurrGame().setMPlayer(1);
        try {
            wait(Math.max(220L, thinkTime() - 80L));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (getWaitCond() == 0) return;
        getCurrGame().setMPlayer(2);
        model.setDiceThrows(getCurrGame().getGameLogic().rollDices());
        getCurrGame().getBoardImage().setDices(model.getDiceThrows());
        getCurrGame().getBoardImage().postInvalidate();
    }
}
