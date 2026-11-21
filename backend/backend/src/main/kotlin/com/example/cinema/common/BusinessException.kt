package com.example.cinema.common

import org.springframework.http.HttpStatus

class BusinessException
    (val status: HttpStatus,
     val code: String,
     override val message: String) : RuntimeException(message) {

}
fun badRequest(code: String,message: String): Nothing =
    throw BusinessException(HttpStatus.BAD_REQUEST,code,message)

fun notFound(message: String) : Nothing =
    throw BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND",message)

fun conflict(code : String , message: String): Nothing =
    throw BusinessException(HttpStatus.CONFLICT,code,message)