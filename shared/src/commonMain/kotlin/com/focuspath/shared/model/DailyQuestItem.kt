package com.focuspath.shared.model

data class DailyQuestItem(
    val title: String,
    val target: Int,
    val current: Int,
    val rewardCoins: Int,
    val isCompleted: Boolean
)
