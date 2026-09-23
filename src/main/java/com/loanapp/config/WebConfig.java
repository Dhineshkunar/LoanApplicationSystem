package com.loanapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class WebConfig {

    /**
     * Single global CORS source for the whole app. Registered before the JWT filter
     * via SecurityConfig's .cors(Customizer.withDefaults()), so no controller or
     * endpoint needs @CrossOrigin.
     *
     * allowedOriginPatterns("*") reflects whatever Origin header the request sends,
     * which is what makes this valid alongside allowCredentials(true) — a literal
     * allowedOrigins("*") is rejected by the CORS spec when credentials are enabled.
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.addAllowedHeader("*");
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setExposedHeaders(Arrays.asList("Content-Type", "Authorization"));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}