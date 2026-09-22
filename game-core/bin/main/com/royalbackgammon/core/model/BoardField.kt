package com.royalbackgammon.core.model

import kotlinx.serialization.Serializable

/**
 * Represents the state of a single point (triangle) on the board.
 * @property chipCount Number of checkers on this point.
 * @property owner Player who owns the checkers (0 = empty).
 */
@Serializable
data class BoardField(
    var chipCount: Int = 0,
    var owner: Int = 0
) {
    companion object {
        const val NOBODY = 0
        const val WHITE = 1
        const val RED = 2
    }
}
