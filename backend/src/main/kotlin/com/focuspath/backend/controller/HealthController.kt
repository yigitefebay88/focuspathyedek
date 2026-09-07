package com.focuspath.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check endpoints")
class HealthController {

    @GetMapping
    @Operation(summary = "Check API health")
    fun healthCheck(): Map<String, String> {
        return mapOf("status" to "UP", "timestamp" to System.currentTimeMillis().toString())
    }
}
