package com.example.cinema.order

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.*
import java.time.LocalDateTime

@Mapper
interface ExpiryMapper {

    @Select("""
        select order_id 
        from ticket_order
        where order_status = 'PENDING' and expires_at <= #{cutoffTime} and order_id > #{afterOrderId}
        order by order_id
        limit 100
    """)
    fun selectExpiredPendingOrderIds(
        @Param("cutoffTime") cutoffTime: LocalDateTime,
        @Param("afterOrderId") afterOrderId: Long,
    ): List<Long>


}