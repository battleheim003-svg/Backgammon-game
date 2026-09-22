package com.royalbackgammon.core.model

/**
 * Per-turn facts that the board alone cannot tell, needed by the running family's head rule.
 * @property headMovesThisTurn Checkers already moved off the head during the current turn.
 * @property firstTurn True during the player's first turn of the game.
 */
data class TurnContext(val headMovesThisTurn: Int = 0, val firstTurn: Boolean = false) {
    companion object {
        @JvmField
        val NONE = TurnContext()
    }
}
