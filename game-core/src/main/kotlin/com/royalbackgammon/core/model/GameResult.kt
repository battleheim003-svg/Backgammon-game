package com.royalbackgammon.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GameResult(val winner: Int, val winType: WinType, val points: Int)
