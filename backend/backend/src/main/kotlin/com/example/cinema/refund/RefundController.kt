package com.example.cinema.refund

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders/{id}")
class RefundController(private val refund: RefundTx) {

    @PostMapping("/refund")
    fun refundOrder(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name ="id") orderId: Long,
        httpServletRequest: HttpServletRequest
    )=refund.refundOrder(principal.userId,orderId,getRequestId(httpServletRequest))

}