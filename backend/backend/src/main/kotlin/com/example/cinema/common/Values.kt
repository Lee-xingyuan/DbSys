package com.example.cinema.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId


fun BigDecimal.asMoney() : String =
    setScale(2, RoundingMode.UNNECESSARY).toPlainString()

fun LocalDateTime.asApiTime() : OffsetDateTime =
    atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime()