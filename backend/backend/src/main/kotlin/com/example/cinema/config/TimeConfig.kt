package com.example.cinema.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class TimeConfig {
    @Bean
    fun businessClock(): Clock =
        Clock.system(ZoneId.of("Asia/Shanghai"))
}