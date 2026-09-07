package com.focuspath.app.core.domain.usecase

class CalculateDopamineUseCase {
    
    companion object {
        const val MIN_MULTIPLIER = 0.5f
        const val MAX_MULTIPLIER = 3.0f
        const val DISTRACTION_PENALTY = 0.1f
        const val BOOST_BONUS = 0.2f
        const val XP_MULTIPLIER_CAP = 1.5f
    }

    fun calculateNewMultiplierAfterDistraction(current: Float): Float {
        return (current - DISTRACTION_PENALTY).coerceAtLeast(MIN_MULTIPLIER)
    }

    fun calculateNewMultiplierAfterBoost(current: Float): Float {
        return (current + BOOST_BONUS).coerceAtMost(MAX_MULTIPLIER)
    }

    fun calculateXpReward(baseXp: Int, currentMultiplier: Float): Int {
        val effectiveMultiplier = if (currentMultiplier > XP_MULTIPLIER_CAP) XP_MULTIPLIER_CAP else currentMultiplier
        return (baseXp * effectiveMultiplier).toInt()
    }
}
