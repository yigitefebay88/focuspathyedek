package com.focuspath.backend.controller

import com.focuspath.backend.model.FriendRequest
import com.focuspath.backend.model.User
import com.focuspath.backend.service.FriendService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/friends")
@Tag(name = "Friends", description = "Friend management")
class FriendController(private val friendService: FriendService) {

    @PostMapping("/request")
    @Operation(summary = "Send a friend request")
    fun sendRequest(@RequestParam senderEmail: String, @RequestParam receiverEmail: String) {
        friendService.sendFriendRequest(senderEmail, receiverEmail)
    }

    @PostMapping("/accept/{requestId}")
    @Operation(summary = "Accept a friend request")
    fun acceptRequest(@PathVariable requestId: Long) {
        friendService.acceptFriendRequest(requestId)
    }

    @GetMapping("/requests")
    @Operation(summary = "Get pending friend requests")
    fun getPendingRequests(@RequestParam email: String): List<FriendRequest> {
        return friendService.getPendingRequests(email)
    }

    @GetMapping
    @Operation(summary = "Get friends list")
    fun getFriends(@RequestParam email: String): List<User> {
        return friendService.getFriends(email)
    }

    @DeleteMapping("/remove")
    @Operation(summary = "Remove a friend")
    fun removeFriend(@RequestParam email: String, @RequestParam friendEmail: String) {
        friendService.removeFriend(email, friendEmail)
    }
}
