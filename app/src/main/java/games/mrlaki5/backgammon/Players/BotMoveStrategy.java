package games.mrlaki5.backgammon.Players;

import java.util.List;
import java.util.Random;

import games.mrlaki5.backgammon.Beans.BoardFieldState;
import games.mrlaki5.backgammon.Beans.DiceThrow;
import games.mrlaki5.backgammon.Beans.NextJump;
import games.mrlaki5.backgammon.GameControllers.GameLogic;
import games.mrlaki5.backgammon.GameControllers.GameMoveExecutor;
import games.mrlaki5.backgammon.GameModel.Model;
import games.mrlaki5.backgammon.GamePreferences;

public class BotMoveStrategy {
    private static final int MAX_TURN_DEPTH = 4;

    public NextJump chooseMove(Model model, List<NextJump> moves, int difficulty, Random random) {
        if (moves == null || moves.isEmpty()) {
            return null;
        }
        if (difficulty == GamePreferences.BOT_EASY) {
            return moves.get(random.nextInt(moves.size()));
        }
        if (difficulty >= GamePreferences.BOT_HARD) {
            NextJump tactical = chooseImmediateTactic(model, moves);
            if (tactical != null) {
                return tactical;
            }
        }

        NextJump best = moves.get(0);
        double bestScore = -Double.MAX_VALUE;
        for (NextJump move : moves) {
            double score;
            if (difficulty == GamePreferences.BOT_MEDIUM) {
                score = scoreSingleMove(model, move, difficulty);
                score += random.nextDouble() * 14.0;
            }
            else {
                score = scoreTurnAfterMove(model, move, difficulty, 1);
                if (difficulty == GamePreferences.BOT_ROYAL) {
                    score -= opponentReplyRiskAfterMove(model, move) * 0.42;
                }
                score += random.nextDouble() * (difficulty == GamePreferences.BOT_HARD ? 3.0 : 1.0);
            }
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    private NextJump chooseImmediateTactic(Model model, List<NextJump> moves) {
        BoardFieldState[] board = model.getBoardFields();
        int opponent = model.getCurrentPlayer() == 1 ? 2 : 1;
        NextJump bestHit = null;
        for (NextJump move : moves) {
            if (move.getDstField() == 26 || move.getDstField() == 27) {
                return move;
            }
            if (board[move.getDstField()].getPlayer() == opponent
                    && board[move.getDstField()].getNumberOfChips() == 1) {
                bestHit = move;
            }
        }
        return bestHit;
    }

    private double scoreTurnAfterMove(Model original, NextJump move, int difficulty, int depth) {
        Model model = copyModel(original);
        new GameMoveExecutor(model).applyMove(move);
        double immediate = evaluateBoard(model, original.getCurrentPlayer(), difficulty);
        if (depth >= MAX_TURN_DEPTH) {
            return immediate;
        }

        GameLogic logic = new GameLogic(model);
        List<NextJump> nextMoves = logic.calculateMoves(model.getBoardFields(),
                model.getCurrentPlayer(), model.getDiceThrows());
        if (nextMoves.isEmpty()) {
            return immediate;
        }

        double bestFuture = -Double.MAX_VALUE;
        for (NextJump next : nextMoves) {
            bestFuture = Math.max(bestFuture, scoreTurnAfterMove(model, next, difficulty,
                    depth + 1));
        }
        return immediate * 0.48 + bestFuture * 0.52;
    }

    private double opponentReplyRiskAfterMove(Model original, NextJump move) {
        Model model = copyModel(original);
        new GameMoveExecutor(model).applyMove(move);
        int player = original.getCurrentPlayer();
        int opponent = player == 1 ? 2 : 1;
        model.setCurrentPlayer(opponent);

        double risk = 0.0;
        for (int first = 1; first <= 6; first++) {
            for (int second = 1; second <= 6; second++) {
                model.setDiceThrows(dice(first, second));
                List<NextJump> replies = new GameLogic(model).calculateMoves(model.getBoardFields(),
                        opponent, model.getDiceThrows());
                double bestReply = 0.0;
                for (NextJump reply : replies) {
                    bestReply = Math.max(bestReply, scoreSingleMove(model, reply,
                            GamePreferences.BOT_HARD));
                }
                risk += bestReply;
            }
        }
        return risk / 36.0;
    }

    private double scoreSingleMove(Model model, NextJump move, int difficulty) {
        Model copy = copyModel(model);
        double tacticalBonus = tacticalMoveBonus(model, move, difficulty);
        new GameMoveExecutor(copy).applyMove(move);
        return evaluateBoard(copy, model.getCurrentPlayer(), difficulty)
                - evaluateBoard(model, model.getCurrentPlayer(), difficulty) * 0.72
                + move.getJumpNumber() * 1.4
                + tacticalBonus;
    }

    private double tacticalMoveBonus(Model model, NextJump move, int difficulty) {
        BoardFieldState[] board = model.getBoardFields();
        int player = model.getCurrentPlayer();
        int opponent = player == 1 ? 2 : 1;
        int dst = move.getDstField();
        double bonus = 0.0;
        if (dst == 26 || dst == 27) {
            bonus += difficulty == GamePreferences.BOT_ROYAL ? 190.0 : 135.0;
        }
        if (board[dst].getPlayer() == opponent && board[dst].getNumberOfChips() == 1) {
            bonus += difficulty >= GamePreferences.BOT_HARD ? 165.0 : 95.0;
        }
        if (board[move.getSrcField()].getNumberOfChips() == 1) {
            bonus += 32.0;
        }
        return bonus;
    }

    private double evaluateBoard(Model model, int player, int difficulty) {
        BoardFieldState[] board = model.getBoardFields();
        int opponent = player == 1 ? 2 : 1;
        GameLogic logic = new GameLogic(model);
        double score = 0.0;

        for (int i = 0; i < board.length; i++) {
            int chips = board[i].getNumberOfChips();
            int owner = board[i].getPlayer();
            if (chips <= 0 || owner == 0) {
                continue;
            }
            int sign = owner == player ? 1 : -1;
            int real = normalizedRealPosition(logic, i, owner);
            boolean home = real >= 19 && real <= 24;
            boolean bar = (owner == 1 && i == 24) || (owner == 2 && i == 25);
            boolean borneOff = (owner == 1 && i == 27) || (owner == 2 && i == 26);

            if (borneOff) score += sign * chips * 110.0;
            else if (bar) score -= sign * chips * 95.0;
            else score += sign * chips * real * 3.2;

            if (!borneOff && !bar) {
                if (chips == 1) score -= sign * blotPenalty(board, i, owner, difficulty);
                if (chips == 2) score += sign * 34.0;
                if (chips >= 3) score += sign * Math.min(42.0, 16.0 + chips * 4.0);
                if (home) score += sign * chips * 7.5;
            }
        }

        score += madePointRun(board, player, logic) * (difficulty == GamePreferences.BOT_ROYAL ? 18.0 : 10.0);
        score -= madePointRun(board, opponent, logic) * 12.0;
        return score;
    }

    private double blotPenalty(BoardFieldState[] board, int field, int owner, int difficulty) {
        int opponent = owner == 1 ? 2 : 1;
        GameLogic logic = new GameLogic(new Model());
        int real = logic.calculateRealPosition(field, owner);
        double penalty = difficulty >= GamePreferences.BOT_HARD ? 34.0 : 22.0;
        for (int i = 0; i < 26; i++) {
            if (board[i].getPlayer() != opponent || board[i].getNumberOfChips() <= 0) {
                continue;
            }
            int oppRealAgainstOwner = 25 - logic.calculateRealPosition(i, opponent);
            int distance = real - oppRealAgainstOwner;
            if (distance >= 1 && distance <= 6) {
                penalty += (7 - distance) * (difficulty == GamePreferences.BOT_ROYAL ? 8.0 : 5.0);
            }
        }
        return penalty;
    }

    private int normalizedRealPosition(GameLogic logic, int field, int owner) {
        int real = logic.calculateRealPosition(field, owner);
        if (real == 100) {
            return 25;
        }
        return real;
    }

    private int madePointRun(BoardFieldState[] board, int player, GameLogic logic) {
        int best = 0;
        int current = 0;
        for (int real = 1; real <= 24; real++) {
            int field = logic.calculateMatrixPosition(real, player);
            if (board[field].getPlayer() == player && board[field].getNumberOfChips() >= 2) {
                current++;
                best = Math.max(best, current);
            }
            else {
                current = 0;
            }
        }
        return best;
    }

    private Model copyModel(Model source) {
        Model copy = new Model();
        BoardFieldState[] board = new BoardFieldState[source.getBoardFields().length];
        for (int i = 0; i < board.length; i++) {
            BoardFieldState field = source.getBoardFields()[i];
            board[i] = new BoardFieldState(field.getNumberOfChips(), field.getPlayer());
        }
        copy.setBoardFields(board);
        copy.setCurrentPlayer(source.getCurrentPlayer());
        copy.setState(source.getState());
        copy.setDiceThrows(copyDice(source.getDiceThrows()));
        return copy;
    }

    private DiceThrow[] copyDice(DiceThrow[] source) {
        DiceThrow[] dice = new DiceThrow[source.length];
        for (int i = 0; i < source.length; i++) {
            dice[i] = new DiceThrow(source[i].getThrowNumber(), source[i].getAlreadyUsed());
        }
        return dice;
    }

    private DiceThrow[] dice(int first, int second) {
        DiceThrow[] dice = new DiceThrow[4];
        dice[0] = new DiceThrow(first);
        dice[1] = new DiceThrow(second);
        if (first == second) {
            dice[2] = new DiceThrow(first);
            dice[3] = new DiceThrow(first);
        }
        else {
            dice[2] = new DiceThrow(0, 1);
            dice[3] = new DiceThrow(0, 1);
        }
        return dice;
    }
}
