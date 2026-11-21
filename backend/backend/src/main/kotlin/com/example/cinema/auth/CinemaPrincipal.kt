package com.example.cinema.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CinemaPrincipal(user: UserRow) :UserDetails{
    val userId : Long = user.userId
    val role : String =user.role;
    private val loginName: String = user.username
    private val enabled : Boolean = user.active

    init{
        require(role == "ADMIN" || role == "CUSTOMER" ){
            "不支持的数据账号角色"
        }

    }

    override fun getUsername(): String = loginName

    // 密码检查在LoginAttemptService中实现
    override fun getPassword(): String? = null

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_$role") )

    override fun isEnabled(): Boolean = enabled

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked() : Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

}