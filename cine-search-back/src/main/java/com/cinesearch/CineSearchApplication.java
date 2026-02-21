package com.cinesearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CineSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CineSearchApplication.class, args);
    }
}
