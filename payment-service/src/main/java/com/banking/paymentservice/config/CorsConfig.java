package com.banking.paymentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer{

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // apply to all endpoints
                        .allowedOrigins("*") // allow all origins (you can restrict later)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*") // allow all headers
                        .allowCredentials(false) // set true only if you need cookies
                        .maxAge(3600); // cache preflight response for 1 hour
            }

}

