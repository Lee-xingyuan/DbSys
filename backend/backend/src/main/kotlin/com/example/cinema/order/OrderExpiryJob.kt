package com.example.cinema.order

import com.example.cinema.common.getCurrentBusinessTime
import java.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OrderExpiryJob(
    private val expiryRepository: ExpiryRepository,
    private val expiryService: OrderExpiryService,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // 应用启动后立即开始扫描
    @Scheduled(initialDelay = 0,fixedDelay = 15000 )
    fun closeExpiredOrderBatch(){
        val cutoffTime=getCurrentBusinessTime(clock)
        var lastOrderId=0L
        repeat(100){ retry->
            val expiredOrderIds=expiryRepository.findExpirePendingOrderIds(cutoffTime,lastOrderId)
            if(expiredOrderIds.isEmpty()) return
            expiredOrderIds.forEach { orderId ->
                try{ expiryService.expireOrder(orderId) }
                catch(exception:Exception){
                    log.error("关闭到期订单失败 orderId={}",orderId, exception)
                }
            }
            lastOrderId=expiredOrderIds.last()
        }
    }
}