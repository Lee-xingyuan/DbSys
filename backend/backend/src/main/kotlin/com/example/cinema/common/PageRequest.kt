package com.example.cinema.common


data class PageRequest(val page:Int=1,val pageSize:Int = 20) {
    init{
        if(page < 1 || pageSize !in 1..100){
            badRequest("INVALID_PAGE", "page 数必须大于等于1，pageSize 必须为1到100！")
        }
    }

    val offset : Long get()=(page.toLong()-1)*pageSize
}
