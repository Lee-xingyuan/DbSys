package com.example.cinema.order

import com.example.cinema.cinema.SeatRow
import com.example.cinema.common.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.annotation.Order
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class LockedClose(private val tradeRepository: TradeRepository){
    @Transactional(propagation = Propagation.MANDATORY)
    fun closePendingOrder(
        order: OrderRow,
        lockedSeats: List<StockRow>,
        currentTime: LocalDateTime,
        requestedCancel:Boolean
    ){
        check(order.orderStatus == "PENDING" && lockedSeats.size in 1..4)
        check(lockedSeats.all {it.screeningId == order.screeningId  && it.currentOrderId == order.orderId && it.inventoryStatus == "HELD"})
        val isExpired = !TradeTime.canPay(currentTime, order.expiresAt,order.startTimeSnapshot)
        check(!isExpired || requestedCancel)
            {"未到期订单不能由超时任务关闭"}
        checkSingleRowAffected(tradeRepository.mapper.updatePendingOrderToClosed(order.orderId,if(isExpired) "EXPIRED" else "CANCELLED",currentTime))
        check(tradeRepository.mapper.updateSeatsToAvailableByOrderIdAndStatus(order.orderId,"HELD")==lockedSeats.size)
    }
}