package com.demo_hotel_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
    "com.demo_hotel_service",
    "com.common.security"
})
@EnableFeignClients(basePackages = "com.demo_hotel_service.clients")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
