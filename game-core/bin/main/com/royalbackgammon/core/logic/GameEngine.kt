package com.royalbackgammon.core.logic

import com.royalbackgammon.core.dice.DiceRoller
import com.royalbackgammon.core.dice.RandomDiceRoller
import com.royalbackgammon.core.model.*

/**
 * High-level game engine that manages the full game lifecycle.
 * Coordinates dice rolling, move calculation, and state transitions.
 *
 * @property state The current game state (mutable).
 * @property diceRoller Injectable dice source.
 */
class GameEngine(
    val state: GameState = GameState.newGame(),
    private val diceRoller: DiceRoller = RandomDiceRoller()
) {

    /**
     * Currently available legal moves for the active player.
     * Recalculated after dice rolls and after each move.
     */
    var legalMoves: List<Move> = emptyList()
        private set

    init {
        // If engine is created with a mid-turn state, calculate legal moves immediately
        if (state.turnState == GameState.STATE_MOVE && state.winner == Player.NONE) {
            recalculateLegalMoves()
        }
    }

    /**
     * Rolls dice for the current turn state. Updates [state.dice] and
     * transitions the FSM appropriately for initial rolls.
     * Returns the rolled values.
     */
    fun rollDice(): Array<Die> {
        return when (state.turnState) {
            GameState.STATE_INITIAL_ROLL_P1 -> {
                val value = diceRoller.rollOne()
                state.dice[0] = Die(value)
                state.dice[1] = Die(state.dice[1].value, state.dice[1].used)
                state.turnState = GameState.STATE_INITIAL_ROLL_P2
                state.switchPlayer()
                state.dice
            }
            GameState.STATE_INITIAL_ROLL_P2 -> {
                val value = diceRoller.rollOne()
                state.dice[0] = Die(state.dice[0].value)
                state.dice[1] = Die(value)
                // Higher roll goes first
                state.currentPlayer = if (state.dice[0].value >= state.dice[1].value) {
                    Player.WHITE
                } else {
                    Player.RED
                }
                state.turnState = GameState.STATE_MOVE
                recalculateLegalMoves()
                state.dice
            }
            GameState.STATE_ROLL -> {
                val rolled = diceRoller.rollTurn()
                for (i in 0 until 4) state.dice[i] = rolled[i]
                state.turnState = GameState.STATE_MOVE
                recalculateLegalMoves()
                state.dice
            }
            else -> state.dice // Already in move state
        }
    }

    /**
     * Attempts to apply a move from [from] to [to].
     * Returns the [MoveResult]. After a successful move, recalculates legal moves.
     * If no more moves are available, ends the turn automatically.
     */
    fun makeMove(from: Int, to: Int): MoveResult {
        val result = MoveExecutor.tryApplyMove(state, from, to, legalMoves)
        if (result.applied) {
            // Check for win
            val winner = BackgammonRules.checkWinner(state.board)
            if (winner != Player.NONE) {
                state.winner = winner
                legalMoves = emptyList()
                return result
            }
            recalculateLegalMoves()
        }
        return result
    }

    /**
     * Applies a [Move] object directly (used by bot).
     * Returns the [MoveResult].
     */
    fun applyMove(move: Move): MoveResult {
        val result = MoveExecutor.applyMove(state, move)
        val winner = BackgammonRules.checkWinner(state.board)
        if (winner != Player.NONE) {
            state.winner = winner
            legalMoves = emptyList()
        } else {
            recalculateLegalMoves()
        }
        return result
    }

    /**
     * Ends the current player's turn and switches to the other player's roll phase.
     * Should be called when [legalMoves] is empty after dice have been rolled.
     */
    fun endTurn() {
        state.switchPlayer()
        state.turnState = GameState.STATE_ROLL
        legalMoves = emptyList()
    }

    /**
     * Returns true if the current turn has no more moves available
     * (dice were rolled but no legal moves exist, or all dice have been consumed).
     */
    fun isTurnComplete(): Boolean = legalMoves.isEmpty() && state.turnState == GameState.STATE_MOVE

    /**
     * Returns true if the game has ended.
     */
    fun isGameOver(): Boolean = state.winner != Player.NONE

    private fun recalculateLegalMoves() {
        legalMoves = BackgammonRules.calculateLegalMoves(
            state.board, state.currentPlayer, state.dice
        )
    }
}
