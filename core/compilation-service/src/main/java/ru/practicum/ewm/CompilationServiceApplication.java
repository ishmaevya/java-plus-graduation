package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "ru.practicum.ewm.common.client")
public class CompilationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompilationServiceApplication.class, args);
    }
}
