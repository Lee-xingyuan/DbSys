package com.example.cinema.order

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders/{id}")
class CancelController(private val cancel: CancelTx) {
    @PostMapping("/cancel")
    fun cancelOrder(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name ="id") orderId: Long
    )=cancel.cancelOrder(principal.userId,orderId)
}