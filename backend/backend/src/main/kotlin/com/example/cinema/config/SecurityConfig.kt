package com.example.cinema.config

import com.example.cinema.auth.CinemaAuthenticationProvider
import com.example.cinema.auth.SecurityResponseWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.savedrequest.NullRequestCache

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity, provider: CinemaAuthenticationProvider, errors: SecurityResponseWriter):
            SecurityFilterChain {
            http.authenticationProvider(provider).csrf (  Customizer.withDefaults() )
                .requestCache { it.requestCache(NullRequestCache())}
                .authorizeHttpRequests { auth ->
                    auth.requestMatchers("/","/index.html","/css/**","/js/**","/images/**","/error"
                    ).permitAll()
                    auth.requestMatchers(
                        HttpMethod.GET,"/api/health","/api/auth/csrf",
                        "/api/movies","/api/movies/**","/api/cinemas","/api/screenings/*/seats"
                    ).permitAll()
                    auth.requestMatchers(
                        HttpMethod.POST,"/api/auth/register","/api/auth/login"
                    ).permitAll()
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN")
                    auth.requestMatchers("/api/**").authenticated()
                    auth.anyRequest().denyAll()
                }
                .formLogin { from->
                    from.loginProcessingUrl("/api/auth/login")
                        .successHandler { _, response, _ -> response.status = 204 }
                    .failureHandler { request, response, _ -> errors.write(request,response,401,"LOGIN_FAILED","用户名或密码有误，或暂时无法登录") }
                }
                .logout{ logout ->
                    logout.logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler{_, response, _ -> response.status = 204}
                }
                .exceptionHandling { handling ->
                    handling.authenticationEntryPoint{request ,response,_ ->
                        errors.write(request,response,401,"LOGIN_REQUIRED","请先登录")
                    }
                    handling.accessDeniedHandler{request, response, _ -> errors.write(request,response,403,"ACCESS_DENIED","无操作权限或安全令牌无效")
                    }
                }
        return http.build()
    }


}