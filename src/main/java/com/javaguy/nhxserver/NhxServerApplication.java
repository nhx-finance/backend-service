package com.javaguy.nhxserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;



@SpringBootApplication
@EnableAsync
public class NhxServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NhxServerApplication.class, args);
    }

}
