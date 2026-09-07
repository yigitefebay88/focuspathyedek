package com.focuspath.backend.repository

import com.focuspath.backend.model.FriendRequest
import com.focuspath.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FriendRequestRepository : JpaRepository<FriendRequest, Long> {
    fun findAllByReceiverAndStatus(receiver: User, status: String): List<FriendRequest>
    fun findBySenderAndReceiverAndStatus(sender: User, receiver: User, status: String): FriendRequest?
}
