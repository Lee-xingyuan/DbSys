package com.example.cinema.common


data class PageResponse<T>
    (val items: List<T> ,
     val page:Int,
     val pageSize:Int ,
     val total:Long )