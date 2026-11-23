package com.example.cinema.order

import com.example.cinema.common.*
import com.zaxxer.hikari.util.ClockSource.currentTime
import java.time.Clock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

fun validateOrderStatus(orderStatus: String?){
    if(orderStatus!=null && orderStatus !in listOf("PENDING","PAID","CANCELLED","EXPIRED","REFUNDED"))
        badRequest("INVALID_STATUS","订单状态无效")
}

@Service
class OrderQuery(
    private val tradeRepository: TradeRepository,
    private val clock: Clock
) {
    //下单请求重发时，查回已经创建的订单
    @Transactional(readOnly = true)
    fun getOrderForIdempotentReplay(
            userId:Long,
            orderRequest: CreateOrderInput
    ):OrderResponse{
        val order=tradeRepository.mapper
            .selectOrderByUserIdAndRequestKey(userId, orderRequest.requestKey.toString()) ?: conflict("RETRY_REQUIRED","并发请求未完成，请用原请求键重试")
        if(order.requestDigest != calculateOrderRequestDigest(orderRequest))
            conflict("REQUEST_KEY_REUSED","请求键绑定了其他内容")
        return tradeRepository.getOrderResponseById(order.orderId , getCurrentBusinessTime(clock))
    }

    @Transactional(readOnly = true)
    fun getUserOrder(userId:Long,orderId:Long)
    :OrderResponse{
        val order=tradeRepository.mapper.selectOrderById(orderId) ?: notFound("订单不存在")
        if(order.userId !=userId)   notFound("订单不存在")
        return tradeRepository.getOrderResponseById(orderId, getCurrentBusinessTime(clock))
    }

    @Transactional(readOnly = true)
    fun listUserOrders(
        userId:Long,
        orderStatus: String?,
        pageRequest: PageRequest
    ):PageResponse<OrderResponse>{
        validateOrderStatus(orderStatus)
        return PageResponse(
            tradeRepository.buildOrderResponses(
                tradeRepository.mapper.selectOrderPageByUserId(userId, orderStatus, pageRequest),
                getCurrentBusinessTime(clock)
            ) ,
            pageRequest.page,
            pageRequest.pageSize,
            tradeRepository.mapper.countOrdersByUserId(userId,orderStatus)
        )
    }
}