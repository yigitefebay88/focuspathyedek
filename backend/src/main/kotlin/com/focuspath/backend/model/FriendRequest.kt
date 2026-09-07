package com.focuspath.backend.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "friend_requests")
data class FriendRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "sender_id")
    val sender: User,

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    val receiver: User,

    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED

    val createdAt: Instant = Instant.now()
)
