package com.example.cinema.common

data class  FieldErrorResponse(val field:String, val message: String)

data class ApiError(val code: String,
               val message: String,
               val requestId: String,
               val fieldErrors: List<FieldErrorResponse> = emptyList()) {
}