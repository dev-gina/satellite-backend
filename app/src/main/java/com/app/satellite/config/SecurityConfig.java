package com.app.satellite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity 
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // CSRF 비활성화
            .authorizeRequests()
                .anyRequest().permitAll() // 모든 요청 허용
            .and()
            .cors(); // CORS 활성화
    }

    // CORS 설정
    @Configuration
    public static class WebConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {

            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000") 
                    .allowedMethods("GET", "POST", "PUT")
                    .allowedHeaders("*")
                    .allowCredentials(true); 
        }
    }
}
