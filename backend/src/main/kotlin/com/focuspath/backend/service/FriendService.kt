package com.focuspath.backend.service

import com.focuspath.backend.model.FriendRequest
import com.focuspath.backend.model.User
import com.focuspath.backend.repository.FriendRequestRepository
import com.focuspath.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FriendService(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository
) {

    @Transactional
    fun sendFriendRequest(senderEmail: String, receiverEmail: String) {
        val sender = userRepository.findByEmail(senderEmail).orElseThrow { Exception("Sender not found") }
        
        // If receiver doesn't exist, create a placeholder
        val receiver = userRepository.findByEmail(receiverEmail).orElseGet {
            userRepository.save(User(email = receiverEmail, username = receiverEmail.substringBefore("@")))
        }

        if (friendRequestRepository.findBySenderAndReceiverAndStatus(sender, receiver, "PENDING") == null) {
            friendRequestRepository.save(FriendRequest(sender = sender, receiver = receiver))
        }
    }

    @Transactional
    fun acceptFriendRequest(requestId: Long) {
        val request = friendRequestRepository.findById(requestId).orElseThrow { Exception("Request not found") }
        val sender = request.sender
        val receiver = request.receiver

        sender.friends.add(receiver)
        receiver.friends.add(sender)

        userRepository.save(sender)
        userRepository.save(receiver)

        friendRequestRepository.save(request.copy(status = "ACCEPTED"))
    }

    fun getPendingRequests(email: String): List<FriendRequest> {
        val user = userRepository.findByEmail(email).orElseThrow { Exception("User not found") }
        return friendRequestRepository.findAllByReceiverAndStatus(user, "PENDING")
    }

    fun getFriends(email: String): List<User> {
        val user = userRepository.findByEmail(email).orElseThrow { Exception("User not found") }
        return user.friends.toList()
    }

    @Transactional
    fun removeFriend(email: String, friendEmail: String) {
        val user = userRepository.findByEmail(email).orElseThrow { Exception("User not found") }
        val friend = userRepository.findByEmail(friendEmail).orElseThrow { Exception("Friend not found") }

        user.friends.remove(friend)
        friend.friends.remove(user)

        userRepository.save(user)
        userRepository.save(friend)
    }
}
