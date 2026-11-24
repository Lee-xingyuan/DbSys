package com.example.cinema.refund

import com.example.cinema.audit.AuditService
import com.example.cinema.common.*
import com.example.cinema.order.RefundResponse
import com.example.cinema.order.RefundRow
import com.example.cinema.order.TradeRepository
import com.example.cinema.order.TradeTime
import com.example.cinema.order.toResponse
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class RefundTx(
    private val tradeRepository: TradeRepository,
    private val audit: AuditService,
    private val clock: Clock
) {
    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun refundOrder(userId: Long,orderId: Long,requestId: String): RefundResponse {
        val order=tradeRepository.lockOrderForOperation(orderId,userId); val tradeMapper=tradeRepository.mapper
        if(order.orderStatus=="REFUNDED") return checkNotNull(tradeMapper.selectRefundByOrderId(orderId)).toResponse()
        if(order.orderStatus!="PAID") conflict("NOT_REFUNDABLE","订单不能退款")
        val lockedSeats=tradeRepository.lockOrderSeatsWithStatus(order,"SOLD"); val currentTime=getCurrentBusinessTime(clock)
        if(!TradeTime.canRefund(currentTime,order.startTimeSnapshot)) conflict("REFUND_DEADLINE_PASSED","已过开场前30分钟退票截止点")
        val successfulPayment=checkNotNull(tradeMapper.selectSuccessfulPaymentByOrderId(orderId)); check(successfulPayment.attemptAmount.compareTo(order.totalAmount)==0)
        val refund=RefundRow().also { it.orderId=orderId; it.paymentAttemptId=successfulPayment.paymentAttemptId; it.refundAmount=order.totalAmount; it.refundedAt=currentTime }
        checkSingleRowAffected(tradeMapper.insertRefundRecord(refund)); check(refund.refundId>0)
        checkSingleRowAffected(tradeMapper.updateOrderToRefunded(orderId)); check(tradeMapper.updateSeatsToAvailableByOrderIdAndStatus(orderId,"SOLD")==lockedSeats.size)
        audit.recordAuditLog(
            actorUserId = userId,
            actionType = "REFUND",
            targetType = "ORDER",
            targetId = orderId,
            occurredAt = currentTime,
            requestId = requestId,
            changeSummary = "整单模拟退款"
        )
        return refund.toResponse()
    }

}