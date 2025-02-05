package org.amine.security.polling.onlinepollingsystem.system.security

import org.amine.security.polling.onlinepollingsystem.system.security.filter.JwtTokenFilter
import org.amine.security.polling.onlinepollingsystem.system.security.services.detailsservice.ApplicationDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
//@EnableMethodSecurity
class SecurityConfig(
    var passWordEncoder: PasswordEncoder,
    var applicationDetailsService: ApplicationDetailsService,
    var jwtTokenFilter: JwtTokenFilter
) {

    @Bean
    fun securityFilterChain(httpSecurity: HttpSecurity): SecurityFilterChain {
        httpSecurity.cors().and().csrf().disable()

            .authorizeHttpRequests()
            // Admin
            .requestMatchers(HttpMethod.POST, "/polling/v1/admins/admin").hasAuthority("WRITE_ADMIN")

            .requestMatchers(HttpMethod.PUT, "/polling/v1/admins/permissions")
            .hasAnyAuthority("WRITE_USER", "WRITE_ADMIN")

            .requestMatchers(HttpMethod.DELETE, "/polling/v1/admins/admin/**").hasAuthority("WRITE_ADMIN")
            .requestMatchers(HttpMethod.POST, "/polling/v1/admins/user").hasAuthority("WRITE_USER")
            .requestMatchers(HttpMethod.DELETE, "/polling/v1/admins/user/**").hasAuthority("WRITE_USER")
            .requestMatchers(HttpMethod.PUT, "/polling/v1/admins/poll/**").hasAuthority("WRITE_POLL")
            .requestMatchers(HttpMethod.DELETE, "/polling/v1/admins/poll/delete").hasAuthority("WRITE_POLL")
            .requestMatchers(HttpMethod.GET, "/polling/v1/admins/**").hasAnyRole("ADMIN")

            .requestMatchers(HttpMethod.POST, "/polling/v1/polls/participate").hasAuthority("PARTICIPATE_POLE")
            .requestMatchers(HttpMethod.POST, "/polling/v1/polls/create").hasAuthority("WRITE_POLE")
            .requestMatchers(HttpMethod.PUT, "/polling/v1/polls/update").hasAuthority("WRITE_POLE")
            .requestMatchers(HttpMethod.PUT, "/polling/v1/polls/open/poll/**").hasAuthority("WRITE_POLE")
            .requestMatchers(HttpMethod.PUT, "/polling/v1/polls/finish/poll/**").hasAuthority("WRITE_POLE")
            .requestMatchers(HttpMethod.PUT, "/polling/v1/polls/cancel/poll/**").hasAuthority("WRITE_POLE")
            .requestMatchers(HttpMethod.PUT, "/polling/v1/polls/pending/poll/**").hasAuthority("WRITE_POLE")
            .requestMatchers(HttpMethod.DELETE, "/polling/v1/polls/delete/poll/**").hasAuthority("WRITE_POLE")

            .requestMatchers("/polling/v1/polls/**").hasAnyRole("USER")
            .requestMatchers("/polling/v1/refreshToken").authenticated()
            .requestMatchers("/polling/v1/auth/**", "/polling/v1/admin/auth/**").permitAll()

            .anyRequest().authenticated()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(
                jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java
            )
            .authenticationProvider(authenticationProvider())

        return httpSecurity.build()
    }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler {
        return CustomAccessDeniedHandler()
    }

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val daoAuthenticationProvider = DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passWordEncoder);
        daoAuthenticationProvider.setUserDetailsService(applicationDetailsService)
        return daoAuthenticationProvider;
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
}