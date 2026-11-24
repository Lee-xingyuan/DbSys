package com.example.cinema.order

import com.example.cinema.auth.CinemaPrincipal
import com.example.cinema.common.PageRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService,private val orderQuery: OrderQuery) {
    @PostMapping
    fun createOrder(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @RequestBody orderRequest: CreateOrderInput
    )=orderService.createOrder(principal.userId,orderRequest)
    @GetMapping
    fun listOrders(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @RequestParam(required=false) status: String?,
        @RequestParam(defaultValue="1") page: Int,
        @RequestParam(defaultValue="20") pageSize: Int
    )=orderQuery.listUserOrders(principal.userId,status,PageRequest(page,pageSize))
    @GetMapping("/{id}")
    fun getOrderDetail(
        @AuthenticationPrincipal principal: CinemaPrincipal,
        @PathVariable(name = "id") orderId: Long
    )=orderQuery.getUserOrder(principal.userId,orderId)
}