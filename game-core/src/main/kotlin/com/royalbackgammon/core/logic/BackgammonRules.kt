package com.royalbackgammon.core.logic

import com.royalbackgammon.core.model.*
import com.royalbackgammon.core.variant.RuleFamily
import com.royalbackgammon.core.variant.Variant

/**
 * Pure game rules engine for all variants. No side effects, no Android dependencies.
 * All methods are stateless — they take the board/state and return computed results.
 */
object BackgammonRules {

    private const val HEAD_REAL = 1
    private const val PRIME_LENGTH = 6

    /**
     * Determines the current game phase for [player] given the [board].
     */
    @JvmStatic
    @JvmOverloads
    fun gamePhase(board: Array<BoardField>, player: Int, variant: Variant = Variant.STANDARD): GamePhase {
        var totalChips = 0
        var allInHome = true

        for (i in 0 until 26) {
            if (board[i].owner == player && board[i].chipCount > 0) {
                totalChips += board[i].chipCount
                val realPos = PositionMapper.toReal(i, player, variant)
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
     *
     * [turn] is only consulted by the running family (head rule).
     */
    @JvmStatic
    @JvmOverloads
    fun calculateLegalMoves(
        board: Array<BoardField>,
        player: Int,
        dice: Array<Die>,
        variant: Variant = Variant.STANDARD,
        turn: TurnContext = TurnContext.NONE
    ): List<Move> {
        val candidates = singleDieMoves(board, player, dice, variant, turn)
        if (candidates.isEmpty()) return candidates

        val remaining = dice.count { !it.used && it.value > 0 }
        if (remaining <= 1) return candidates

        val depths = IntArray(candidates.size)
        var best = 0
        for ((index, move) in candidates.withIndex()) {
            val depth = 1 + maxDiceAfter(board, player, dice, move, remaining - 1, variant, turn)
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

    /** Matrix index of [player]'s starting point in the running family. */
    @JvmStatic
    fun headIndex(player: Int, variant: Variant): Int =
        PositionMapper.toMatrix(HEAD_REAL, player, variant)

    /**
     * Moves playable with a single unused die, ignoring the maximum-dice rule.
     * Duplicate dice (doubles) yield each move once.
     */
    private fun singleDieMoves(
        board: Array<BoardField>,
        player: Int,
        dice: Array<Die>,
        variant: Variant,
        turn: TurnContext
    ): List<Move> {
        val phase = gamePhase(board, player, variant)
        if (phase == GamePhase.FINISHED) return emptyList()

        val dieValues = dice.filter { !it.used && it.value > 0 }.map { it.value }.distinct()
        if (dieValues.isEmpty()) return emptyList()

        return when (variant.family) {
            RuleFamily.HITTING -> hittingMoves(board, player, dieValues, phase, variant)
            RuleFamily.RUNNING -> runningMoves(board, player, dice, dieValues, phase, variant, turn)
        }
    }

    private fun hittingMoves(
        board: Array<BoardField>,
        player: Int,
        dieValues: List<Int>,
        phase: GamePhase,
        variant: Variant
    ): List<Move> {
        val moves = mutableListOf<Move>()
        val barIndex = GameState.barIndex(player)
        val mustPlayBar = board[barIndex].chipCount > 0
        val farthestBack = if (phase == GamePhase.BEARING_OFF) farthestBackReal(board, player, variant) else -1

        val sources = if (mustPlayBar) listOf(barIndex) else (0 until 24).toList()
        for (i in sources) {
            if (board[i].owner != player || board[i].chipCount <= 0) continue
            val realPos = PositionMapper.toReal(i, player, variant)

            for (dieValue in dieValues) {
                val realNext = realPos + dieValue
                if (realNext > 24) {
                    bearOffMove(player, i, realPos, dieValue, phase, farthestBack)?.let { moves.add(it) }
                } else {
                    val dst = PositionMapper.toMatrix(realNext, player, variant)
                    if (board[dst].owner == player || board[dst].chipCount <= 1) {
                        moves.add(Move(dieValue = dieValue, from = i, to = dst))
                    }
                }
            }
        }
        return moves
    }

    private fun runningMoves(
        board: Array<BoardField>,
        player: Int,
        dice: Array<Die>,
        dieValues: List<Int>,
        phase: GamePhase,
        variant: Variant,
        turn: TurnContext
    ): List<Move> {
        val moves = mutableListOf<Move>()
        val head = headIndex(player, variant)
        val headBlocked = turn.headMovesThisTurn >= headLimit(dice, turn)
        val farthestBack = if (phase == GamePhase.BEARING_OFF) farthestBackReal(board, player, variant) else -1

        for (i in 0 until 24) {
            if (board[i].owner != player || board[i].chipCount <= 0) continue
            if (i == head && headBlocked) continue
            val realPos = PositionMapper.toReal(i, player, variant)

            for (dieValue in dieValues) {
                val realNext = realPos + dieValue
                if (realNext > 24) {
                    bearOffMove(player, i, realPos, dieValue, phase, farthestBack)?.let { moves.add(it) }
                    continue
                }
                val dst = PositionMapper.toMatrix(realNext, player, variant)
                if (board[dst].chipCount > 0 && board[dst].owner != player) continue
                if (trapsAllOpponents(board, player, i, dst, variant)) continue
                moves.add(Move(dieValue = dieValue, from = i, to = dst))
            }
        }
        return moves
    }

    private fun bearOffMove(
        player: Int, from: Int, realPos: Int, dieValue: Int, phase: GamePhase, farthestBack: Int
    ): Move? {
        if (phase != GamePhase.BEARING_OFF) return null
        // Overshoot only from the farthest-back checker
        if (realPos + dieValue != 25 && realPos != farthestBack) return null
        return Move(dieValue = dieValue, from = from, to = GameState.bearOffIndex(player))
    }

    /** Two checkers may leave the head on a first-turn 6-6, 4-4 or 3-3; otherwise one. */
    private fun headLimit(dice: Array<Die>, turn: TurnContext): Int {
        val first = dice[0].value
        val isDouble = first > 0 && dice.all { it.value == first }
        return if (turn.firstTurn && isDouble && first in setOf(3, 4, 6)) 2 else 1
    }

    /**
     * True if moving a checker [from] → [to] leaves [player] with six consecutive points in the
     * opponent's path while no opponent checker is past them.
     */
    private fun trapsAllOpponents(
        board: Array<BoardField>, player: Int, from: Int, to: Int, variant: Variant
    ): Boolean {
        val opponent = Player.opponent(player)
        fun ownedAfterMove(index: Int): Boolean {
            val count = board[index].chipCount + (if (index == to) 1 else 0) - (if (index == from) 1 else 0)
            return count > 0 && (board[index].owner == player || index == to)
        }

        var run = 0
        var runEnd = -1
        for (oppReal in 1..24) {
            if (ownedAfterMove(PositionMapper.toMatrix(oppReal, opponent, variant))) {
                run++
                if (run >= PRIME_LENGTH) runEnd = oppReal
            } else {
                run = 0
            }
        }
        if (runEnd == -1) return false

        if (board[GameState.bearOffIndex(opponent)].chipCount > 0) return false
        for (i in 0 until 24) {
            if (board[i].owner == opponent && board[i].chipCount > 0 &&
                PositionMapper.toReal(i, opponent, variant) > runEnd
            ) {
                return false
            }
        }
        return true
    }

    private fun farthestBackReal(board: Array<BoardField>, player: Int, variant: Variant): Int {
        var farthest = Int.MAX_VALUE
        for (i in 0 until 24) {
            if (board[i].owner == player && board[i].chipCount > 0) {
                val real = PositionMapper.toReal(i, player, variant)
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
        cap: Int,
        variant: Variant,
        turn: TurnContext
    ): Int {
        if (cap == 0) return 0
        val state = GameState(
            board = Array(board.size) { board[it].copy() },
            dice = Array(dice.size) { dice[it].copy() },
            currentPlayer = player,
            turnState = GameState.STATE_MOVE,
            variant = variant,
            headMovesThisTurn = turn.headMovesThisTurn
        )
        MoveExecutor.applyMove(state, move)
        val nextTurn = turn.copy(headMovesThisTurn = state.headMovesThisTurn)

        val next = singleDieMoves(state.board, player, state.dice, variant, nextTurn)
        var best = 0
        for (nextMove in next) {
            val depth = 1 + maxDiceAfter(state.board, player, state.dice, nextMove, cap - 1, variant, nextTurn)
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
     * Classifies a finished game for [winner]: single, gammon (mars) or backgammon.
     * The running family has no backgammon.
     */
    @JvmStatic
    @JvmOverloads
    fun winType(board: Array<BoardField>, winner: Int, variant: Variant = Variant.STANDARD): WinType {
        val loser = Player.opponent(winner)
        if (board[GameState.bearOffIndex(loser)].chipCount > 0) return WinType.SINGLE
        if (variant.family == RuleFamily.RUNNING) return WinType.GAMMON

        if (board[GameState.barIndex(loser)].chipCount > 0) return WinType.BACKGAMMON
        for (i in 0 until 24) {
            if (board[i].owner == loser && board[i].chipCount > 0 &&
                PositionMapper.toReal(i, loser, variant) in 1..6
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
        val type = winType(board, winner, variant)
        return GameResult(winner = winner, winType = type, points = variant.scoring.pointsFor(type))
    }
}
