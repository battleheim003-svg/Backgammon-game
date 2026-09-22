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

/** Movement family: decides direction, contact and blocking rules. */
enum class RuleFamily {
    /** Opposite directions, a lone checker can be hit to the bar. */
    HITTING,
    /**
     * Same rotational direction from diagonally opposite heads, no hitting, one checker owns a
     * point, one checker leaves the head per turn, no prime that traps all opponent checkers.
     */
    RUNNING
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

/** Playable rule variants. */
enum class Variant(
    val family: RuleFamily,
    val openingRoll: OpeningRoll,
    val scoring: WinScoring,
    val cubeAllowed: Boolean
) {
    STANDARD(RuleFamily.HITTING, OpeningRoll.PLAY_OPENING_DICE, WinScoring(1, 2, 3), cubeAllowed = true),
    TAVLA(RuleFamily.HITTING, OpeningRoll.STARTER_REROLLS, WinScoring(1, 2, 2), cubeAllowed = false),
    PORTES(RuleFamily.HITTING, OpeningRoll.STARTER_REROLLS, WinScoring(1, 2, 2), cubeAllowed = false),
    /** Long Nardy: oyn 1, mars 2. */
    NARDY(RuleFamily.RUNNING, OpeningRoll.STARTER_REROLLS, WinScoring(1, 2, 2), cubeAllowed = false);
}
