package com.example.cinema.order

import com.example.cinema.common.PageRequest
import com.example.cinema.screening.InventoryRow
import java.time.LocalDateTime
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
@Mapper
interface TradeMapper {

    fun selectOrderById(orderId:Long):OrderRow?

    fun selectOrderByIdForUpdate(orderId:Long):OrderRow?

    fun selectOrderByUserIdAndRequestKey(
        @Param("userId") userId:Long,
        @Param("requestKey") requestKey:String
    ):OrderRow?

    fun selectOrderByUserIdAndRequestKeyForUpdate(
    @Param("userId") userId:Long,
    @Param("requestKey") requestKey:String
    ):OrderRow?

    fun insertPendingOrder(orderRow: OrderRow):Int

    fun insertOrderItem(orderItem: ItemRow):Int

    fun selectOrderItemsByOrderIds(
        @Param("orderIds") orderIds:List<Long>
    ):List<ItemRow>

    fun selectSeatLabelByScreeningSeatId(screeningSeatId:Long): InventoryRow?

    fun selectScreeningSeatIdsByOrderId(orderId:Long):List<Long>

    fun selectScreeningSeatsByIdsForUpdate(
        @Param("screeningId") screeningId:Long,
        @Param("screeningSeatsId") screeningSeatIds:List<Long>
    ):List<StockRow>

    fun updateSeatToHeld(
        @Param("orderId") orderId:Long,
        @Param("screeningSeatId") screeningSeatId:Long,
        @Param("screeningId") screeningId:Long
    ):Int

    fun updateSeatsToSoldByOrderId(orderId: Long): Int

    fun updateSeatsToAvailableByOrderIdAndStatus(
        @Param("orderId") orderId: Long,
        @Param("expectedInventoryStatus") expectedInventoryStatus: String
    ): Int

    fun updateOrderToPaid(@Param("orderId") orderId: Long,
                          @Param("paidAt") paidAt: LocalDateTime
    ): Int

    fun updatePendingOrderToClosed(
        @Param("orderId") orderId: Long,
        @Param("targetStatus") targetStatus: String,
        @Param("closedAt") closedAt: LocalDateTime
    ): Int

    fun selectSuccessfulPaymentByOrderId(orderId: Long): PaymentRow?

    fun insertPaymentAttempt(paymentAttempt: PaymentRow): Int

    fun selectRefundByOrderId(orderId: Long): RefundRow?

    fun selectRefundsByOrderIds(
        @Param("orderIds") orderIds: List<Long>
    ): List<RefundRow>

    fun insertRefundRecord(refundRecord: RefundRow): Int

    fun selectOrderPageByUserId(
        @Param("userId") userId: Long,
        @Param("orderStatus") orderStatus: String?,
        @Param("pageRequest") pageRequest: PageRequest
    ): List<OrderRow>

    fun countOrdersByUserId(
        @Param("userId") userId: Long,
        @Param("orderStatus") orderStatus: String?
    ): Long


}