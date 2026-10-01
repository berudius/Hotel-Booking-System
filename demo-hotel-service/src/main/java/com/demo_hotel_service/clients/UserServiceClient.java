package com.demo_hotel_service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.demo_hotel_service.config.feignconfig.UserClientFeignConfig;

@FeignClient(name = "user-service", path = "api/internal", configuration = UserClientFeignConfig.class) 
public interface UserServiceClient {
    
    @GetMapping("/users/exists-by-phone")
    boolean checkPhoneExists(@RequestParam("phoneNumber") String phoneNumber);
}
