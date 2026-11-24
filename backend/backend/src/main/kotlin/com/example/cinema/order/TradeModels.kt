package com.example.cinema.order

import java.math.BigDecimal
import java.time.*
import java.util.UUID

class OrderRow{
    var orderId = 0L; var userId=0L; var screeningId=0L;
    var requestKey=""; var requestDigest=""; var orderStatus=""
    var totalAmount=BigDecimal.ZERO ; var createdAt=LocalDateTime.MIN; var expiresAt=LocalDateTime.MIN
    var paidAt:LocalDateTime?=null; ;  var closedAt:LocalDateTime?=null
    var movieTitleSnapshot=""; var cinemaNameSnapshot=""; var hallNameSnapshot=""
    var startTimeSnapshot=LocalDateTime.MIN;  var endTimeSnapshot=LocalDateTime.MIN
}

class StockRow{
    var screeningSeatId=0L ;var screeningId=0L;
    var inventoryStatus=""
    var currentOrderId:Long?=null
}

class ItemRow{
    var orderId=0L; var screeningSeatId=0L;
    var seatLabelSnapshot=""
    var ticketPrice=BigDecimal.ZERO
}

class PaymentRow{
    var paymentAttemptId=0L; var orderId=0L; var requestKey=""
    var attemptedAt=LocalDateTime.MIN ; var attemptAmount=BigDecimal.ZERO
    var paymentResult=""
}

class RefundRow{
    var  refundId=0L; var paymentAttemptId=0L; var orderId=0L;
    var refundAmount=BigDecimal.ZERO
    var refundedAt=LocalDateTime.MIN
}

data class CreateOrderInput(
    val screeningId: Long,
    val revision: Int,
    val screeningSeatIds: List<Long>,
    val requestKey: UUID
)
enum class PaymentResult { SUCCESS, FAILURE }

data class PayInput(val requestKey: UUID, val simulationResult: PaymentResult)

data class ItemResponse(val screeningSeatId: Long,
                        val seatLabel: String,
                        val ticketPrice: String)

data class RefundResponse(
    val refundId: Long,
    val orderId: Long,
    val paymentAttemptId: Long,
    val refundAmount: String,
    val refundedAt: OffsetDateTime
)

data class PaymentResponse(
    val paymentAttemptId: Long,
    val orderId: Long,
    val requestKey: String,
    val attemptedAt: OffsetDateTime,
    val amount: String,
    val paymentResult: String
)

data class OrderResponse(
    val orderId:Long,
    val screeningId: Long,
    val status:String,
    val movieTitle: String,
    val cinemaName: String,
    val hallName: String,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val items:List<ItemResponse>,
    val totalAmount: String,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    val paidAt: OffsetDateTime?,
    val closedAt: OffsetDateTime?,
    val refund:RefundResponse?,
    val serverTime:OffsetDateTime,
    val refundAble:Boolean,
    val refundDate:OffsetDateTime
)

data class OperationResponse(
    val order:OrderResponse,
    val payment:PaymentResponse?=null
)

sealed interface TradeOutcome{
    data class Done(val response: OperationResponse):TradeOutcome
    data class Expired(val order:OrderResponse):TradeOutcome
}

object TradeTime {
    fun calculateExpiresAt(createdAt: LocalDateTime,startsAt: LocalDateTime)=
        minOf(createdAt.plusMinutes(10),startsAt.minusMinutes(5))

    fun canPay(currentTime: LocalDateTime,expiresAt: LocalDateTime,startsAt: LocalDateTime)=
        currentTime<expiresAt && currentTime<startsAt.minusMinutes(5)

    fun canRefund(currentTime: LocalDateTime,startsAt: LocalDateTime)=
        currentTime<startsAt.minusMinutes(30)
}

fun calculateOrderRequestDigest(order: CreateOrderInput)
    : String= java.security.MessageDigest.getInstance("SHA-256")
                .digest("${order.screeningId}:${order.revision}:${order.screeningSeatIds.sorted().joinToString(",")}"
                .toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 255) }