package com.api.identity;

import com.api.identity.configuration.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class})
public class UserApplication {
    static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
