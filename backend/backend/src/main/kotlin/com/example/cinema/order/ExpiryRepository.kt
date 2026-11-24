package com.example.cinema.order

import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExpiryRepository(private val mapper: ExpiryMapper) {

    fun findExpirePendingOrderIds(
        cutoffTime: LocalDateTime,
        afterOrderId: Long,
    )=mapper.selectExpiredPendingOrderIds(cutoffTime, afterOrderId)

}