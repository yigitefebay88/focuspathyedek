package com.focuspath.app.data.model

data class Team(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val creatorUid: String = "",
    val memberEmails: List<String> = emptyList(),
    val weeklyXpGoal: Long = 5000L,
    val currentWeeklyXp: Long = 0L,
    val totalTeamXp: Long = 0L,
    val badges: List<String> = emptyList(),
    val mvpEmail: String? = null,
    val lastGoalResetTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
