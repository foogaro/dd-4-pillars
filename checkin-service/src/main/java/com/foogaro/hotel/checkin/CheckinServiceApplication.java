package com.foogaro.hotel.checkin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.foogaro.hotel")
public class CheckinServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckinServiceApplication.class, args);
    }
}
