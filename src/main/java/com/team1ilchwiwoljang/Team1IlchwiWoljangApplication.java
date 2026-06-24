package com.team1ilchwiwoljang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class Team1IlchwiWoljangApplication {

    public static void main(String[] args) {
        SpringApplication.run(Team1IlchwiWoljangApplication.class, args);
    }

}
