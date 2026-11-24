package com.example.cinema.order

import com.example.cinema.common.*
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class CancelTx(
    private val tradeRepository: TradeRepository,
    private val orderCloser: LockedClose,
    private val clock: Clock
) {
    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun cancelOrder(userId: Long,orderId: Long)
    : OrderResponse {
        val order=tradeRepository.lockOrderForOperation(orderId,userId)
        if(order.orderStatus in listOf("CANCELLED","EXPIRED"))
            return tradeRepository.getOrderResponseById(orderId,getCurrentBusinessTime(clock))
        if(order.orderStatus!="PENDING")
            conflict("ORDER_NOT_PENDING","当前订单不能取消")
        val lockedSeats=tradeRepository.lockOrderSeatsWithStatus(order,"HELD")
        val currentTime=getCurrentBusinessTime(clock)
        orderCloser.closePendingOrder(order,lockedSeats,currentTime,true)
        return tradeRepository.getOrderResponseById(orderId,currentTime)
    }
}