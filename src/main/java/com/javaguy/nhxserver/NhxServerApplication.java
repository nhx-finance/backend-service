package com.javaguy.nhxserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootApplication
@EnableAsync
public class NhxServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NhxServerApplication.class, args);
    }

}
