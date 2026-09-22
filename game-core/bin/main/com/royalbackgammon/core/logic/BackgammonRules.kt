package com.royalbackgammon.core.logic

import com.royalbackgammon.core.model.*

/**
 * Pure game rules engine for backgammon. No side effects, no Android dependencies.
 * All methods are stateless — they take the board/state and return computed results.
 */
object BackgammonRules {

    /**
     * Determines the current game phase for [player] given the [board].
     */
    @JvmStatic
    fun gamePhase(board: Array<BoardField>, player: Int): GamePhase {
        var totalChips = 0
        var allInHome = true

        for (i in 0 until 26) {
            if (board[i].owner == player && board[i].chipCount > 0) {
                totalChips += board[i].chipCount
                val realPos = PositionMapper.toReal(i, player)
                if (realPos !in 19..24) {
                    allInHome = false
                }
            }
        }

        return when {
            allInHome && totalChips == 0 -> GamePhase.FINISHED
            allInHome -> GamePhase.BEARING_OFF
            else -> GamePhase.PLAYING
        }
    }

    /**
     * Calculates all legal moves for [player] given the current [board] and [dice].
     * Returns an empty list if no moves are available.
     */
    @JvmStatic
    fun calculateLegalMoves(
        board: Array<BoardField>,
        player: Int,
        dice: Array<Die>
    ): List<Move> {
        val phase = gamePhase(board, player)
        if (phase == GamePhase.FINISHED) return emptyList()

        val moves = mutableListOf<Move>()
        val barIndex = GameState.barIndex(player)

        // If player has checkers on bar, only those can move
        val mustPlayBar = board[barIndex].chipCount > 0

        // Find the lowest real position with a checker (for bearing-off overshoot rule)
        var farthestBackReal = -1

        // Iterate fields
        val startIndex = if (mustPlayBar) barIndex else 0
        val endIndex = if (mustPlayBar) barIndex + 1 else 26

        for (i in startIndex until endIndex) {
            if (board[i].owner != player || board[i].chipCount <= 0) continue

            val realPos = PositionMapper.toReal(i, player)

            // Track farthest-back checker for bearing-off logic
            if (farthestBackReal == -1 && realPos in 1..24) {
                farthestBackReal = realPos
            }

            for (die in dice) {
                if (die.used || die.value == 0) continue

                val realNext = realPos + die.value

                if (realNext > 24) {
                    // Can only bear off if in bearing-off phase
                    if (phase != GamePhase.BEARING_OFF) continue

                    // Overshoot: allowed only if exact (realNext == 25) or this is the farthest-back checker
                    if (realNext != 25 && realPos != farthestBackReal) continue

                    val dstMatrix = GameState.bearOffIndex(player)
                    moves.add(Move(dieValue = die.value, from = i, to = dstMatrix))
                } else {
                    val dstMatrix = PositionMapper.toMatrix(realNext, player)
                    // Destination must be: empty, owned by us, or have at most 1 opponent checker (hittable)
                    if (board[dstMatrix].owner == player || board[dstMatrix].chipCount <= 1) {
                        moves.add(Move(dieValue = die.value, from = i, to = dstMatrix))
                    }
                }
            }
        }

        return moves
    }

    /**
     * Calculates legal moves for a specific source field.
     * Returns the set of destination indices, or null if no moves from that field.
     */
    @JvmStatic
    fun movesFromField(allMoves: List<Move>, fromField: Int): Set<Int>? {
        val destinations = allMoves.filter { it.from == fromField }.map { it.to }.toSet()
        return destinations.ifEmpty { null }
    }

    /**
     * Checks if a move (from → to) is present in the legal moves list.
     */
    @JvmStatic
    fun isLegalMove(from: Int, to: Int, legalMoves: List<Move>): Move? {
        return legalMoves.find { it.from == from && it.to == to }
    }

    /**
     * Determines the winner of the game (if any). Returns 0 if game is ongoing.
     */
    @JvmStatic
    fun checkWinner(board: Array<BoardField>): Int {
        if (gamePhase(board, Player.WHITE) == GamePhase.FINISHED) return Player.WHITE
        if (gamePhase(board, Player.RED) == GamePhase.FINISHED) return Player.RED
        return Player.NONE
    }
}
