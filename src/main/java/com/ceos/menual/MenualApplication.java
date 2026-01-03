package com.ceos.menual;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MenualApplication {

    public static void main(String[] args) {
        SpringApplication.run(MenualApplication.class, args);
    }

}
