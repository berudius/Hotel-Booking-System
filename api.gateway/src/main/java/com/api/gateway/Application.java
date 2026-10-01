package com.api.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.api.gateway",
    "com.common.security"
})
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
