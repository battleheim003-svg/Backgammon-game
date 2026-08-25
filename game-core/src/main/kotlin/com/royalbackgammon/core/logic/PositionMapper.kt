package com.royalbackgammon.core.logic

import com.royalbackgammon.core.model.GameState
import com.royalbackgammon.core.model.Player

/**
 * Converts between internal matrix positions (0–27) and "real" positions
 * relative to a player's direction of travel (1–24, 0=bar, 100=borne off).
 *
 * Board indexing:
 *  - Matrix 0..11  → White real 12..1 (descending)
 *  - Matrix 12..23 → White real 13..24 (ascending)
 *  - Matrix 24 = White bar (real 0)
 *  - Matrix 25 = Red bar (real 0 for Red)
 *  - Matrix 26 = Red bear-off (real 100 for Red)
 *  - Matrix 27 = White bear-off (real 100 for White)
 *
 * Red sees the board flipped: Red's real = 25 − White's real.
 */
object PositionMapper {

    /**
     * Converts matrix position to "real" position for [player].
     * Real positions: 0 = bar, 1–24 = board points, 100 = borne off.
     */
    fun toReal(matrixPos: Int, player: Int): Int {
        if (player == Player.RED) {
            return when (matrixPos) {
                GameState.RED_BAR -> 0
                GameState.RED_BEAR_OFF -> 100
                else -> 25 - toReal(matrixPos, Player.WHITE)
            }
        }
        // White
        return when (matrixPos) {
            GameState.WHITE_BAR -> 0
            GameState.WHITE_BEAR_OFF -> 100
            in 0..11 -> 12 - matrixPos
            else -> matrixPos + 1  // 12..23 → 13..24
        }
    }

    /**
     * Converts "real" position for [player] back to matrix position.
     */
    fun toMatrix(realPos: Int, player: Int): Int {
        if (player == Player.RED) {
            return when (realPos) {
                0 -> GameState.RED_BAR
                100 -> GameState.RED_BEAR_OFF
                else -> toMatrix(25 - realPos, Player.WHITE)
            }
        }
        // White
        return when (realPos) {
            0 -> GameState.WHITE_BAR
            100 -> GameState.WHITE_BEAR_OFF
            in 1..12 -> 12 - realPos
            else -> realPos - 1  // 13..24 → 12..23
        }
    }
}
