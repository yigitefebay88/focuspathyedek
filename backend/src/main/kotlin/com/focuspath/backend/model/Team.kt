package com.focuspath.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "teams")
data class Team(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,

    val description: String? = null,

    var totalTeamXp: Long = 0,

    val adminEmail: String,

    @OneToMany(mappedBy = "team")
    val members: MutableList<User> = mutableListOf()
)
