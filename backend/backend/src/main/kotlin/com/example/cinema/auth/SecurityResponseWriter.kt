package com.example.cinema.auth

import com.example.cinema.common.ApiError
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestAttributes
import tools.jackson.databind.ObjectMapper

@Component
class SecurityResponseWriter(private val mapper: ObjectMapper)  {
    fun write(req: HttpServletRequest,
              res:HttpServletResponse,
              status:Int,
              code: String,
              msg:String
    ) {
        val requestId = req.getAttribute("requestId") as? String ?: "unavailable"
        res.status = status
        res.characterEncoding = "UTF-8"
        res.contentType = "application/json"
        res.writer.write(mapper.writeValueAsString(ApiError(code,msg,requestId)))
    }
}