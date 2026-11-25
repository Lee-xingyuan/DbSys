package com.example.cinema.order

import com.example.cinema.common.*
import java.sql.SQLException
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.TransientDataAccessException
import org.springframework.stereotype.Service

@Service
class OrderService(private val creationService: CreateOrderTx, private val orderQuery: OrderQuery
) {
    fun createOrder(userId:Long , orderRequest:CreateOrderInput)
    : OrderResponse{
        repeat(3){ retry->
            try{
                return creationService.createOrder(userId, orderRequest)
            }
            catch (exception:DuplicateKeyException){
                val sqlException = generateSequence<Throwable>(exception) {it.cause}
                    .filterIsInstance<SQLException>()
                    .firstOrNull{it.errorCode == 1062}
                // create.sql的UNIQUE(user_id,request_key)由MySQL命名为user_id。
                // 仅这个索引走恢复，order_item等其他唯一键错误继续抛出。
                val keyName = sqlException?.message.orEmpty()
                    .substringAfterLast(" for key '", "")
                    .substringBefore("'", "")

                if (keyName != "ticket_order.user_id" && keyName != "user_id")
                    throw exception

                return orderQuery.getOrderForIdempotentReplay(userId, orderRequest)
            }
            catch (exception:TransientDataAccessException){
                if(retry == 2)
                    conflict("RETRY_REQUIRED","数据库繁忙，请用原请求键重试")
            }
        }
        error("unreachable")
    }
}