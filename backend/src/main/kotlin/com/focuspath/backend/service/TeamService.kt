package com.focuspath.backend.service

import com.focuspath.backend.model.Team
import com.focuspath.backend.model.User
import com.focuspath.backend.repository.TeamRepository
import com.focuspath.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createTeam(name: String, description: String, adminEmail: String): Team {
        val admin = userRepository.findByEmail(adminEmail).orElseThrow { Exception("Admin not found") }
        val team = teamRepository.save(Team(name = name, description = description, adminEmail = adminEmail))
        
        admin.team = team
        userRepository.save(admin)
        
        return team
    }

    @Transactional
    fun joinTeam(teamId: Long, email: String) {
        val user = userRepository.findByEmail(email).orElseThrow { Exception("User not found") }
        val team = teamRepository.findById(teamId).orElseThrow { Exception("Team not found") }
        
        user.team = team
        userRepository.save(user)
    }

    fun getLeaderboard(): List<Team> {
        return teamRepository.findAllByOrderByTotalTeamXpDesc()
    }

    fun getTeamForUser(email: String): Team? {
        val user = userRepository.findByEmail(email).orElse(null)
        return user?.team
    }
}
