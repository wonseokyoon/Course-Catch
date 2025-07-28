package com.Catch_Course.global.init;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
public class DevInitData {

    @Bean
    public ApplicationRunner devApplicationRunner() {
        return args -> {
            System.out.println("dev application runner");
        };
    }
}