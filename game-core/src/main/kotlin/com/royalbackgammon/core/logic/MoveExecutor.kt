package com.royalbackgammon.core.logic

import com.royalbackgammon.core.model.*

/**
 * Applies moves to a [GameState]. Mutates the state in place.
 * No Android dependencies.
 */
object MoveExecutor {

    /**
     * Applies a [move] to the given [state].
     * The move must be legal (caller should validate with BackgammonRules first).
     * Returns a [MoveResult] indicating success and whether an opponent checker was hit.
     */
    @JvmStatic
    fun applyMove(state: GameState, move: Move): MoveResult {
        val board = state.board
        val player = state.currentPlayer
        val src = move.from
        val dst = move.to

        // Remove checker from source
        board[src].chipCount--
        if (board[src].chipCount == 0) {
            board[src].owner = Player.NONE
        }

        // Consume the die
        consumeDie(state, move.dieValue)

        // Place checker at destination (handles hits)
        val hit = placeChecker(state, dst, player)

        return MoveResult(applied = true, from = src, to = dst, hit = hit)
    }

    /**
     * Validates and applies a move. Returns a failed MoveResult if the move is not in [legalMoves].
     */
    @JvmStatic
    fun tryApplyMove(state: GameState, from: Int, to: Int, legalMoves: List<Move>): MoveResult {
        val move = BackgammonRules.isLegalMove(from, to, legalMoves)
            ?: return MoveResult(applied = false, from = from, to = to)
        return applyMove(state, move)
    }

    private fun consumeDie(state: GameState, dieValue: Int) {
        for (die in state.dice) {
            if (die.value == dieValue && !die.used) {
                die.used = true
                return
            }
        }
    }

    private fun placeChecker(state: GameState, dstIndex: Int, player: Int): Boolean {
        val board = state.board
        val opponent = Player.opponent(player)
        val hit = board[dstIndex].chipCount == 1 && board[dstIndex].owner == opponent

        if (hit) {
            // Send opponent's checker to the bar
            val opponentBar = GameState.barIndex(opponent)
            board[opponentBar].chipCount++
            board[opponentBar].owner = opponent
            // Replace the checker at destination
            board[dstIndex].chipCount = 1
            board[dstIndex].owner = player
        } else {
            board[dstIndex].chipCount++
            board[dstIndex].owner = player
        }

        return hit
    }
}
