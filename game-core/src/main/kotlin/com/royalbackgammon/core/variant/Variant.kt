package com.royalbackgammon.core.variant

import com.royalbackgammon.core.model.WinType
import kotlinx.serialization.Serializable

/** How the first turn of a game starts after the one-die opening roll decides who moves first. */
enum class OpeningRoll {
    /** Starter plays the two opening dice (international backgammon). */
    PLAY_OPENING_DICE,
    /** Starter rolls both dice again; doubles are possible on the first move (Tavla, Portes). */
    STARTER_REROLLS
}

/** Points awarded per [WinType]. */
@Serializable
data class WinScoring(val single: Int, val gammon: Int, val backgammon: Int) {
    fun pointsFor(type: WinType): Int = when (type) {
        WinType.SINGLE -> single
        WinType.GAMMON -> gammon
        WinType.BACKGAMMON -> backgammon
    }
}

/**
 * Rule variants sharing the hitting-family movement (opposite directions, hit to bar, bear off).
 * Pinning (Plakoto) and running (Nardy, Fevga) families are added in later phases.
 */
enum class Variant(
    val openingRoll: OpeningRoll,
    val scoring: WinScoring,
    val cubeAllowed: Boolean
) {
    STANDARD(OpeningRoll.PLAY_OPENING_DICE, WinScoring(1, 2, 3), cubeAllowed = true),
    TAVLA(OpeningRoll.STARTER_REROLLS, WinScoring(1, 2, 2), cubeAllowed = false),
    PORTES(OpeningRoll.STARTER_REROLLS, WinScoring(1, 2, 2), cubeAllowed = false);
}
