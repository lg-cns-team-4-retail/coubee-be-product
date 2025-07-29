package com.coubee.coubeebeproduct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CoubeeBeProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoubeeBeProductApplication.class, args);
    }

}
