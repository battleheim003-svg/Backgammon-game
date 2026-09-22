package com.royalbackgammon.core.logic

import com.royalbackgammon.core.model.*
import com.royalbackgammon.core.variant.Variant

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
     * Calculates the legal next checker moves for [player] with the unused [dice].
     *
     * Enforces the maximum-dice rule: only moves that begin a sequence using the greatest
     * possible number of dice are legal, and when only one die of a non-double roll can be
     * played, the higher die must be played if possible.
     */
    @JvmStatic
    fun calculateLegalMoves(
        board: Array<BoardField>,
        player: Int,
        dice: Array<Die>
    ): List<Move> {
        val candidates = singleDieMoves(board, player, dice)
        if (candidates.isEmpty()) return candidates

        val remaining = dice.count { !it.used && it.value > 0 }
        if (remaining <= 1) return candidates

        val depths = IntArray(candidates.size)
        var best = 0
        for ((index, move) in candidates.withIndex()) {
            val depth = 1 + maxDiceAfter(board, player, dice, move, remaining - 1)
            depths[index] = depth
            if (depth > best) best = depth
        }

        val legal = candidates.filterIndexed { index, _ -> depths[index] == best }
        if (best == 1) {
            val highest = legal.maxOf { it.dieValue }
            return legal.filter { it.dieValue == highest }
        }
        return legal
    }

    /**
     * Moves playable with a single unused die, ignoring the maximum-dice rule.
     * Duplicate dice (doubles) yield each move once.
     */
    private fun singleDieMoves(board: Array<BoardField>, player: Int, dice: Array<Die>): List<Move> {
        val phase = gamePhase(board, player)
        if (phase == GamePhase.FINISHED) return emptyList()

        val dieValues = dice.filter { !it.used && it.value > 0 }.map { it.value }.distinct()
        if (dieValues.isEmpty()) return emptyList()

        val moves = mutableListOf<Move>()
        val barIndex = GameState.barIndex(player)
        val mustPlayBar = board[barIndex].chipCount > 0
        val farthestBackReal = if (phase == GamePhase.BEARING_OFF) farthestBackReal(board, player) else -1

        val sources = if (mustPlayBar) listOf(barIndex) else (0 until 24).toList()
        for (i in sources) {
            if (board[i].owner != player || board[i].chipCount <= 0) continue
            val realPos = PositionMapper.toReal(i, player)

            for (dieValue in dieValues) {
                val realNext = realPos + dieValue
                if (realNext > 24) {
                    if (phase != GamePhase.BEARING_OFF) continue
                    // Overshoot only from the farthest-back checker
                    if (realNext != 25 && realPos != farthestBackReal) continue
                    moves.add(Move(dieValue = dieValue, from = i, to = GameState.bearOffIndex(player)))
                } else {
                    val dst = PositionMapper.toMatrix(realNext, player)
                    if (board[dst].owner == player || board[dst].chipCount <= 1) {
                        moves.add(Move(dieValue = dieValue, from = i, to = dst))
                    }
                }
            }
        }
        return moves
    }

    private fun farthestBackReal(board: Array<BoardField>, player: Int): Int {
        var farthest = Int.MAX_VALUE
        for (i in 0 until 24) {
            if (board[i].owner == player && board[i].chipCount > 0) {
                val real = PositionMapper.toReal(i, player)
                if (real < farthest) farthest = real
            }
        }
        return farthest
    }

    /** Greatest number of additional dice playable after [move], capped at [cap]. */
    private fun maxDiceAfter(
        board: Array<BoardField>,
        player: Int,
        dice: Array<Die>,
        move: Move,
        cap: Int
    ): Int {
        if (cap == 0) return 0
        val state = GameState(
            board = Array(board.size) { board[it].copy() },
            dice = Array(dice.size) { dice[it].copy() },
            currentPlayer = player,
            turnState = GameState.STATE_MOVE
        )
        MoveExecutor.applyMove(state, move)

        val next = singleDieMoves(state.board, player, state.dice)
        var best = 0
        for (nextMove in next) {
            val depth = 1 + maxDiceAfter(state.board, player, state.dice, nextMove, cap - 1)
            if (depth > best) best = depth
            if (best == cap) break
        }
        return best
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

    /**
     * Classifies a finished game for [winner]: single, gammon or backgammon.
     */
    @JvmStatic
    fun winType(board: Array<BoardField>, winner: Int): WinType {
        val loser = Player.opponent(winner)
        if (board[GameState.bearOffIndex(loser)].chipCount > 0) return WinType.SINGLE

        if (board[GameState.barIndex(loser)].chipCount > 0) return WinType.BACKGAMMON
        for (i in 0 until 24) {
            if (board[i].owner == loser && board[i].chipCount > 0 &&
                PositionMapper.toReal(i, loser) in 1..6
            ) {
                return WinType.BACKGAMMON
            }
        }
        return WinType.GAMMON
    }

    /**
     * Returns the scored result of the game under [variant], or null while the game is ongoing.
     */
    @JvmStatic
    fun gameResult(board: Array<BoardField>, variant: Variant): GameResult? {
        val winner = checkWinner(board)
        if (winner == Player.NONE) return null
        val type = winType(board, winner)
        return GameResult(winner = winner, winType = type, points = variant.scoring.pointsFor(type))
    }
}
