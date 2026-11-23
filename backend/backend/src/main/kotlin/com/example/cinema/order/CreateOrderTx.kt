package com.example.cinema.order

import com.example.cinema.common.*
import com.example.cinema.screening.ScreeningRepository
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class CreateOrderTx(
    private val tradeRepository: TradeRepository,
    private val screeningRepository: ScreeningRepository,
    private val clock: Clock
) {
    @Transactional(isolation=Isolation.READ_COMMITTED)
    fun createOrder(
        userId: Long,
        orderRequest: CreateOrderInput
    ): OrderResponse {
        if(orderRequest.screeningId<=0 ||
            orderRequest.revision<=0 ||
            orderRequest.screeningSeatIds.size !in 1..4 ||
            orderRequest.screeningSeatIds.any { it<=0 } ||
            orderRequest.screeningSeatIds.distinct().size!=orderRequest.screeningSeatIds.size)
                 badRequest("INVALID_SEATS","请选择1～4个不同有效座位和有效场次")

        val screening=tradeRepository.lockScreeningById(orderRequest.screeningId)
        val tradeMapper=tradeRepository.mapper
        val existing=tradeMapper.selectOrderByUserIdAndRequestKeyForUpdate(userId,orderRequest.requestKey.toString())

        if(existing!=null) {
            if(existing.requestDigest!=calculateOrderRequestDigest(orderRequest))
                conflict("REQUEST_KEY_REUSED","请求键绑定了其他内容")
                return tradeRepository.getOrderResponseById(existing.orderId,getCurrentBusinessTime(clock))
        }

        if(screening.revision!=orderRequest.revision)
            conflict("SCREENING_CHANGED","场次已变更，请刷新")

        val selectedSeats=tradeMapper
            .selectScreeningSeatsByIdsForUpdate(screening.screeningId,
                                                orderRequest.screeningSeatIds.sorted())

        if(selectedSeats.size!=orderRequest.screeningSeatIds.size ||
            selectedSeats.any { it.inventoryStatus!="AVAILABLE" || it.currentOrderId!=null })
                conflict("SEAT_CONFLICT","座位不可售")

        val currentTime=getCurrentBusinessTime(clock)

        if(screening.screeningStatus!="SCHEDULED" || currentTime >= screening.startsAt.minusMinutes(5))
            conflict("SALES_STOPPED","场次停售")

        val screeningView=checkNotNull(screeningRepository.mapper.selectScreeningViewById(screening.screeningId))

        val newOrder=OrderRow().also {
            it.userId=userId;
            it.screeningId=screening.screeningId;
            it.requestKey=orderRequest.requestKey.toString()
            it.requestDigest=calculateOrderRequestDigest(orderRequest)
            it.totalAmount=screening.ticketPrice.multiply(java.math.BigDecimal.valueOf(selectedSeats.size.toLong()))
            it.createdAt=currentTime; it.expiresAt=TradeTime.calculateExpiresAt(currentTime,screening.startsAt)
            it.movieTitleSnapshot=screeningView.title; it.cinemaNameSnapshot=screeningView.cinemaName; it.hallNameSnapshot=screeningView.hallName
            it.startTimeSnapshot=screening.startsAt; it.endTimeSnapshot=screening.endsAt
        }

        checkSingleRowAffected(tradeMapper.insertPendingOrder(newOrder)); check(newOrder.orderId>0)

        selectedSeats.forEach { stock ->
            val seatLabel=checkNotNull(tradeMapper.selectSeatLabelByScreeningSeatId(stock.screeningSeatId))
            checkSingleRowAffected(tradeMapper.insertOrderItem(ItemRow().also {
                it.orderId=newOrder.orderId;
                it.screeningSeatId=stock.screeningSeatId;
                it.seatLabelSnapshot="${seatLabel.rowNumber}排${seatLabel.seatNumber}座";
                it.ticketPrice=screening.ticketPrice })
            )
            checkSingleRowAffected(tradeMapper.updateSeatToHeld(newOrder.orderId,stock.screeningSeatId,screening.screeningId))
        }
        return tradeRepository.getOrderResponseById(newOrder.orderId,currentTime)
    }

}