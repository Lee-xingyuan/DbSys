package com.example.cinema.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class HealthResponse(val status: String)

@RestController
class HealthController {
    @GetMapping("/api/health")
    fun health(): HealthResponse = HealthResponse(status = "UP")
}