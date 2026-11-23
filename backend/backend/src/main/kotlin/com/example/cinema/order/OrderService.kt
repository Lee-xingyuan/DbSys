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
                if(
                    sqlException == null || !(
                            sqlException.message.orEmpty().contains("ticket_order.user_id") ||
                            sqlException.message.orEmpty().contains("user_id")
                            )
                )
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