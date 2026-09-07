package com.focuspath.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val email: String = "",

    val username: String = "",

    var xp: Long = 0,

    var coins: Int = 0,

    var level: Int = 1,

    @Column(length = 2048)
    var photoUrl: String? = null,

    @ManyToMany
    @JoinTable(
        name = "user_friends",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "friend_id")]
    )
    @com.fasterxml.jackson.annotation.JsonIgnore
    val friends: MutableSet<User> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    var team: Team? = null
)
