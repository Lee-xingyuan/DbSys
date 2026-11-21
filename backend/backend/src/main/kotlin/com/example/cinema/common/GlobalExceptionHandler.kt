package com.example.cinema.common

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log= LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun business(ex: BusinessException,req: HttpServletRequest): ResponseEntity<ApiError> =
            ResponseEntity.status(ex.status).body(
            ApiError(ex.code,ex.message,id(req))
        )
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException,req: HttpServletRequest):
            ResponseEntity<ApiError> =
            ResponseEntity.badRequest().body(ApiError("VALIDATION_FAILED","请检查输入字段",id(req),
                ex.bindingResult.fieldErrors.map{
                    FieldErrorResponse(it.field,it.defaultMessage ?: "字段不合法")
                })
            )

    @ExceptionHandler(HttpMessageNotReadableException::class,MethodArgumentTypeMismatchException::class)
    fun malformed(ex: Exception,req: HttpServletRequest):
            ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError("INVALID_REQUEST","请求格式或者参数类型不正确",id(req)))

    @ExceptionHandler(Exception::class)
    fun unexpected(ex:Exception,req: HttpServletRequest):
    ResponseEntity<ApiError> {
        val requestId = id(req)
        if(ex is ErrorResponse && ex.statusCode.is4xxClientError) {
            return ResponseEntity.status(ex.statusCode).headers(ex.headers).body(
                ApiError("INVALID_REQUEST","请求参数、方法或资源不正确",requestId))
        }
        log.error("请求失败 requestId={}", requestId,ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError("INTERNAL_ERROR","服务暂时不可用，请稍后重试",requestId))
    }


    private fun id(req: HttpServletRequest): String =
        req.getAttribute("requestId") as? String ?: "unavailable"

}