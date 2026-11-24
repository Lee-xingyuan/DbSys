package com.example.cinema.order

import com.example.cinema.common.getCurrentBusinessTime
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

    @Service
class OrderExpiryService(
    private val tradeRepository: TradeRepository,
    private val orderCloser: LockedClose,
    private val clock: Clock
) {
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun expireOrder(
        orderId: Long
    ){
        if(tradeRepository.mapper.selectOrderById(orderId)==null)       return
        val order=tradeRepository.lockOrderForOperation(orderId,null)
        if(order.orderStatus !="PENDING")   return
        val lockedSeats = tradeRepository.lockOrderSeatsWithStatus(order,"HELD")
        val currentTime = getCurrentBusinessTime(clock)

        if(TradeTime.canPay(currentTime,order.expiresAt,order.startTimeSnapshot))    return

        orderCloser.closePendingOrder(order,lockedSeats,currentTime,false)
    }

}