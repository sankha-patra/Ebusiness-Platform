package com.ebusiness.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableScheduling
public class EbusinessPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbusinessPlatformApplication.class, args);
    }
}
