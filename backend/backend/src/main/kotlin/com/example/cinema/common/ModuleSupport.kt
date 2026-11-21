package com.example.cinema.common

import com.example.cinema.auth.CinemaPrincipal
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.*

val zone = ZoneId.of("Asia/Shanghai")

fun getCurrentBusinessTime(clock : Clock) : LocalDateTime =
    LocalDateTime.ofInstant(clock.instant(), zone).withNano(0)

fun calculatePercentage(n:Long , d: Long): String? =
    if(d == 0L) null
    else
        BigDecimal.valueOf(n).multiply(BigDecimal("100"))
            .divide(BigDecimal.valueOf(d),1,RoundingMode.HALF_UP)
            .toPlainString()

fun trimAndValidateText(s:String ,max:Int) : String=
    s.trim().also{
        if(it.isEmpty() || it.codePointCount(0,it.length) > max)
            badRequest("INVALID_TEXT","文本为空或者过长")
    }

fun checkSingleRowAffected(n: Int) { check(n == 1) { "数据库写入数量异常：$n" } }

fun getRequestId(r: HttpServletRequest): String =
    (r.getAttribute("requestId") as? String) ?: "unavailable"

fun requireAdminUserId(p: CinemaPrincipal): Long{
    if(p.role != "ADMIN")
        throw BusinessException(HttpStatus.FORBIDDEN , "ACCESS_DENIED" , "需要管理员权限")
    return p.userId
}

data class DateWindow(val from:LocalDate, val to: LocalDate){
    init{
        if(to < from || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 365 )
                badRequest("INVALID_DATE_RANGE","日期范围需要为1~365天")
    }
    val start:LocalDateTime get() = from.atStartOfDay()
    val end:LocalDateTime get() = to.plusDays(1).atStartOfDay()
}






