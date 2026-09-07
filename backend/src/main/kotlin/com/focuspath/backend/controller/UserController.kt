package com.focuspath.backend.controller

import com.focuspath.backend.dto.UserSyncRequest
import com.focuspath.backend.model.User
import com.focuspath.backend.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management and sync")
class UserController(private val userService: UserService) {

    @PostMapping("/sync")
    @Operation(summary = "Sync user statistics")
    fun syncUser(@RequestBody request: UserSyncRequest): User {
        return userService.syncUser(request)
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get global leaderboard")
    fun getLeaderboard(): List<User> {
        return userService.getLeaderboard()
    }

    @GetMapping("/search")
    @Operation(summary = "Search user by email")
    fun searchUser(@RequestParam email: String): User? {
        return userService.getUserByEmail(email)
    }
}
