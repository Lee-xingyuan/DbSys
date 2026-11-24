package com.example.cinema.payment

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.*
import com.example.cinema.order.OperationResponse
import com.example.cinema.order.PayInput
import com.example.cinema.order.TradeOutcome
import com.example.cinema.order.toResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders/{id}")
class PaymentController(private val payment: PaymentTx) {
    // 仅课程模拟支付。真实系统不能接受客户端的SUCCESS作为收款凭据。
    @PostMapping("/payments")
    fun simulateOrderPayment(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") orderId: Long,
        @RequestBody paymentRequest: PayInput,
        httpRequest: HttpServletRequest
    ): OperationResponse =
        when(val result=payment.simulateOrderPayment(principal.userId,orderId,paymentRequest,getRequestId(httpRequest))) {
            is TradeOutcome.Done -> result.response
            // 此时注入的事务Bean已正常返回，关闭和释放已提交；409在事务外抛出。
            is TradeOutcome.Expired -> conflict("ORDER_EXPIRED","订单已过期并释放座位")
        }
}