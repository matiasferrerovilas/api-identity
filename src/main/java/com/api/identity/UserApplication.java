package com.api.identity;

import com.api.identity.configuration.WebBindingRuntimeHints;
import com.api.identity.configuration.properties.CorsProperties;
import com.api.identity.configuration.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@ImportRuntimeHints(WebBindingRuntimeHints.class)
public class UserApplication {
    static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
