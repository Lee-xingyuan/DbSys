package com.example.cinema.payment

import com.example.cinema.audit.AuditService
import com.example.cinema.common.*
import com.example.cinema.order.LockedClose
import com.example.cinema.order.OperationResponse
import com.example.cinema.order.PayInput
import com.example.cinema.order.PaymentResult
import com.example.cinema.order.PaymentRow
import com.example.cinema.order.TradeOutcome
import com.example.cinema.order.TradeRepository
import com.example.cinema.order.TradeTime
import com.example.cinema.order.toResponse
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentTx(
    private val tradeRepository: TradeRepository,
    private val orderCloser: LockedClose,
    private val audit: AuditService,
    private val clock: Clock
) {
    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun simulateOrderPayment(
        userId: Long,
        orderId:Long,
        paymentRequest: PayInput,
        requestId:String
    ):TradeOutcome{
        val order=tradeRepository.lockOrderForOperation(orderId,userId)
        val tradeMapper=tradeRepository.mapper
        val existingPayment=tradeMapper
                .selectPaymentAttemptByOrderIdAndRequestKey(orderId, paymentRequest.requestKey.toString())
        if(existingPayment != null) {
            if(existingPayment.paymentResult != paymentRequest.simulationResult.name )
                conflict("PAYMENT_KEY_REUSED","支付请求键不同")
            return TradeOutcome.Done(OperationResponse(
                                                tradeRepository.getOrderResponseById(orderId,
                                                getCurrentBusinessTime(clock)),
                                                existingPayment.toResponse()
            ))
        }
        if(order.orderStatus=="PAID")

            return TradeOutcome.Done(OperationResponse(
                tradeRepository.getOrderResponseById(orderId,getCurrentBusinessTime(clock)),
            ))

        if(order.orderStatus != "PENDING")
            conflict("ORDER_NOT_PENDING","当前订单不能支付")
        val lockedSeats =tradeRepository.lockOrderSeatsWithStatus(order,"HELD")
        val currentTime=getCurrentBusinessTime(clock)

        if(!TradeTime.canPay(currentTime,order.expiresAt,order.startTimeSnapshot)){
            orderCloser.closePendingOrder(order,lockedSeats,currentTime,false)
            return TradeOutcome.Expired(tradeRepository.getOrderResponseById(orderId,currentTime))
        }

        val payment= PaymentRow().also{
            it.orderId = orderId ; it.requestKey = paymentRequest.requestKey.toString();
            it.attemptedAt =currentTime ; it.attemptAmount=order.totalAmount
            it.paymentResult=paymentRequest.simulationResult.name
        }

        checkSingleRowAffected(tradeMapper.insertPaymentAttempt(payment))
        check(payment.paymentAttemptId>0)
        if(paymentRequest.simulationResult == PaymentResult.SUCCESS){
            checkSingleRowAffected(tradeMapper.updateOrderToPaid(orderId,currentTime))
            check(tradeMapper.updateSeatsToSoldByOrderId(orderId)==lockedSeats.size)
            audit.recordAuditLog(
                actorUserId = userId,
                actionType = "SIMULATED_PAYMENT",
                targetType = "ORDER",
                targetId = orderId,
                occurredAt = currentTime,
                requestId = requestId,
                changeSummary = "模拟支付结果：${paymentRequest.simulationResult}"
            )
        }
        return TradeOutcome.Done(
            OperationResponse(
                tradeRepository.getOrderResponseById(orderId,currentTime),
                    payment.toResponse()
                    )
        )

    }
}