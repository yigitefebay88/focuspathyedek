package com.focuspath.backend.repository

import com.focuspath.backend.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamRepository : JpaRepository<Team, Long> {
    fun findByName(name: String): Team?
    fun findAllByOrderByTotalTeamXpDesc(): List<Team>
}
