package com.focuspath.backend.controller

import com.focuspath.backend.model.Team
import com.focuspath.backend.service.TeamService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Team management")
class TeamController(private val teamService: TeamService) {

    @PostMapping
    @Operation(summary = "Create a new team")
    fun createTeam(@RequestParam name: String, @RequestParam description: String, @RequestParam adminEmail: String): Team {
        return teamService.createTeam(name, description, adminEmail)
    }

    @PostMapping("/join")
    @Operation(summary = "Join a team")
    fun joinTeam(@RequestParam teamId: Long, @RequestParam email: String) {
        teamService.joinTeam(teamId, email)
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get team leaderboard")
    fun getLeaderboard(): List<Team> {
        return teamService.getLeaderboard()
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's team")
    fun getMyTeam(@RequestParam email: String): Team? {
        return teamService.getTeamForUser(email)
    }
}
