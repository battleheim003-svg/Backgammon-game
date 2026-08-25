package com.royalbackgammon.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Complete, serializable state of a backgammon game.
 *
 * Board layout (28 fields):
 *  - Indices 0–23: the 24 points (triangles)
 *  - Index 24: White's bar
 *  - Index 25: Red's bar
 *  - Index 26: Red's bear-off
 *  - Index 27: White's bear-off
 *
 * @property board Array of 28 board fields.
 * @property dice Array of 4 dice (supports doubles).
 * @property currentPlayer Currently active player (1=White, 2=Red).
 * @property turnState FSM state: 0=P1 initial roll, 1=P2 initial roll, 2=move, 3=roll.
 * @property winner 0 if game is ongoing, otherwise the winning player number.
 */
@Serializable
data class GameState(
    val board: Array<BoardField> = Array(28) { BoardField() },
    val dice: Array<Die> = Array(4) { Die(value = 0, used = true) },
    var currentPlayer: Int = Player.WHITE,
    var turnState: Int = STATE_INITIAL_ROLL_P1,
    var winner: Int = Player.NONE
) {
    companion object {
        const val STATE_INITIAL_ROLL_P1 = 0
        const val STATE_INITIAL_ROLL_P2 = 1
        const val STATE_MOVE = 2
        const val STATE_ROLL = 3

        // Board indices
        const val WHITE_BAR = 24
        const val RED_BAR = 25
        const val RED_BEAR_OFF = 26
        const val WHITE_BEAR_OFF = 27

        fun barIndex(player: Int): Int = if (player == Player.WHITE) WHITE_BAR else RED_BAR
        fun bearOffIndex(player: Int): Int = if (player == Player.WHITE) WHITE_BEAR_OFF else RED_BEAR_OFF

        /**
         * Creates a new game with the standard backgammon starting position.
         */
        fun newGame(): GameState {
            val state = GameState()
            // White pieces
            state.board[0].chipCount = 5; state.board[0].owner = Player.WHITE
            state.board[11].chipCount = 2; state.board[11].owner = Player.WHITE
            state.board[16].chipCount = 3; state.board[16].owner = Player.WHITE
            state.board[18].chipCount = 5; state.board[18].owner = Player.WHITE
            // Red pieces
            state.board[4].chipCount = 3; state.board[4].owner = Player.RED
            state.board[6].chipCount = 5; state.board[6].owner = Player.RED
            state.board[12].chipCount = 5; state.board[12].owner = Player.RED
            state.board[23].chipCount = 2; state.board[23].owner = Player.RED
            return state
        }

        private val json = Json { prettyPrint = false }

        /**
         * Deserializes a GameState from JSON.
         */
        fun fromJson(jsonString: String): GameState = json.decodeFromString(jsonString)
    }

    /**
     * Serializes this game state to JSON.
     */
    fun toJson(): String = json.encodeToString(this)

    /**
     * Creates a deep copy of this game state.
     */
    fun deepCopy(): GameState = GameState(
        board = Array(board.size) { board[it].copy() },
        dice = Array(dice.size) { dice[it].copy() },
        currentPlayer = currentPlayer,
        turnState = turnState,
        winner = winner
    )

    /**
     * Switches the current player to the opponent.
     */
    fun switchPlayer() {
        currentPlayer = Player.opponent(currentPlayer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return board.contentEquals(other.board) &&
                dice.contentEquals(other.dice) &&
                currentPlayer == other.currentPlayer &&
                turnState == other.turnState &&
                winner == other.winner
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + dice.contentHashCode()
        result = 31 * result + currentPlayer
        result = 31 * result + turnState
        result = 31 * result + winner
        return result
    }
}
