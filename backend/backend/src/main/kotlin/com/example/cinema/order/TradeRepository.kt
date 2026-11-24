package com.example.cinema.order

import com.example.cinema.common.*
import com.example.cinema.screening.ScreeningRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.ResponseBody


fun RefundRow.toResponse()=
    RefundResponse(refundId,orderId,paymentAttemptId,refundAmount.asMoney(),refundedAt.asApiTime())

fun PaymentRow.toResponse()
    =PaymentResponse(paymentAttemptId,orderId,requestKey,attemptedAt.asApiTime(),attemptAmount.asMoney(),paymentResult)

@Repository
class TradeRepository(val mapper: TradeMapper , val screeningRepository: ScreeningRepository) {

    fun lockScreeningById(screeningId: Long)=
        screeningRepository.mapper.selectScreeningByIdForUpdate(screeningId) ?: notFound("场次不存在")


    fun lockOrderForOperation(orderId: Long,userId: Long?): OrderRow {
        val locatedOrder=mapper.selectOrderById(orderId) ?: notFound("订单不存在")
        lockScreeningById(locatedOrder.screeningId)
        val order=mapper.selectOrderByIdForUpdate(orderId) ?: notFound("订单不存在")
        check(order.screeningId==locatedOrder.screeningId)
        if(userId!=null && order.userId!=userId)     notFound("订单不存在")
        return order
    }


    fun lockOrderSeatsWithStatus(
        order: OrderRow,
        expectedInventoryStatus: String
    ): List<StockRow> {
        val screeningSeatIds=mapper.selectScreeningSeatIdsByOrderId(order.orderId)
        check(screeningSeatIds.size in 1..4 &&
                screeningSeatIds.distinct().size==screeningSeatIds.size)

        val lockedSeats=
            mapper.selectScreeningSeatsByIdsForUpdate(order.screeningId,screeningSeatIds.sorted())
        check(lockedSeats.size==screeningSeatIds.size &&
                lockedSeats.all {
                    it.screeningId==order.screeningId &&
                    it.currentOrderId==order.orderId &&
                    it.inventoryStatus==expectedInventoryStatus
                }) { "订单与库存不一致" }
        return lockedSeats
    }

    fun buildOrderResponses(
        orders: List<OrderRow>,
        currentTime: LocalDateTime
    ): List<OrderResponse> {
        if(orders.isEmpty()) return emptyList()

        val orderIds=orders.map { it.orderId }
        val itemsByOrderId=mapper.selectOrderItemsByOrderIds(orderIds)
            .groupBy { it.orderId };
        val refundsByOrderId=mapper.selectRefundsByOrderIds(orderIds).associateBy { it.orderId }

        return orders.map {
            order -> OrderResponse(
            order.orderId,
            order.screeningId,
            order.orderStatus,
            order.movieTitleSnapshot,
            order.cinemaNameSnapshot,
            order.hallNameSnapshot,
            order.startTimeSnapshot.asApiTime(),
            order.endTimeSnapshot.asApiTime(),
            itemsByOrderId[order.orderId].orEmpty().map {
                ItemResponse(it.screeningSeatId,it.seatLabelSnapshot,it.ticketPrice.asMoney())
            },
            order.totalAmount.asMoney(),
            order.createdAt.asApiTime(),
            order.expiresAt.asApiTime(),
            order.paidAt?.asApiTime(),
            order.closedAt?.asApiTime(),
            refundsByOrderId[order.orderId]?.toResponse(),
            currentTime.asApiTime(),
            order.orderStatus=="PAID" && TradeTime.canRefund(currentTime, order.startTimeSnapshot),
            order.startTimeSnapshot.minusMinutes(30).asApiTime()) }
    }

    fun getOrderResponseById(
        orderId: Long,
        currentTime: LocalDateTime
    )=buildOrderResponses(listOf(mapper.selectOrderById(orderId) ?: notFound("订单不存在")), currentTime).single()



}