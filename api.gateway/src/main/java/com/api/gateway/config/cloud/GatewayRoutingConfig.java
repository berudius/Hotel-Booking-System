package com.api.gateway.config.cloud;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;


import com.common.security.AuthPrincipal;
import com.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GatewayRoutingConfig {
    private final JwtUtil jwtUtil;
    @Value("${booking_service_url}")
    private String bookingServiceUrl;
    @Value("${user_service_url}")
    private String userServiceUrl;
    @Value("${resource_service_url}")
    private String resourceServiceUrl;

    @Bean
    public RouterFunction<ServerResponse> bookingRoute() {
        return buildServiceRoute("/api/bookings/**", bookingServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> bookingAdminRoute() {
        return buildServiceRoute("/api/admin/bookings**", bookingServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> roomRoute() {
        return buildServiceRoute("/api/rooms/**", bookingServiceUrl);
    }
    @Bean
    public RouterFunction<ServerResponse> spaRoute() {
        return buildServiceRoute("/api/spas/**", bookingServiceUrl);
    }
    @Bean
    public RouterFunction<ServerResponse> imageRoute() {
        return buildServiceRoute("/api/image/**", bookingServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> uploadsRoute() {
        return buildPublicServiceRoute("/uploads/**", bookingServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> claimManualRoute() {
        return buildPublicServiceRoute("/api/bookings/claim-manual", bookingServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> signUpRoute() {
        return buildPublicServiceRoute("/api/sign_up**", userServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> spaWorkersRoute() {
        return buildServiceRoute("/api/spa-workers/**", resourceServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> physicalUnitsRoute() {
        return buildServiceRoute("/api/physical-units/**", resourceServiceUrl);
    }

    @Bean
    public RouterFunction<ServerResponse> allocationsRoute() {
        return buildServiceRoute("/api/allocations/**", resourceServiceUrl); 
    }

    @Bean
    public RouterFunction<ServerResponse> serviceUnitsSearchRoute() {
        return buildServiceRoute("/api/service-units/search**", bookingServiceUrl); 
    }
    
    @Bean
    public RouterFunction<ServerResponse> serviceUnitsSearchByIdsRoute() {
        return buildServiceRoute("/api/service-units/short-by-ids**", bookingServiceUrl); 
    }
    @Bean
    public RouterFunction<ServerResponse> serviceAllocationsCalendarRoute() {
        return buildServiceRoute("/api/allocations/calendar", resourceServiceUrl); 
    }
    
    @Bean
    public RouterFunction<ServerResponse> userProfileRoute() {
        // Будь-які запити на /api/users/** йдуть в user-service
        return buildServiceRoute("/api/users/**", userServiceUrl); 
    }

    @Bean
    public RouterFunction<ServerResponse> requestRegistrationOtpRoute() {
        return buildServiceRoute("/api/request-registration-otp", userServiceUrl); 
    }
    @Bean
    public RouterFunction<ServerResponse> requestResetPasswordOtpRoute() {
        return buildServiceRoute("/api/request-reset-otp", userServiceUrl); 
    }
    @Bean
    public RouterFunction<ServerResponse> requestResetPasswordRoute() {
        return buildServiceRoute("/api/reset-password", userServiceUrl); 
    }
    
    private RouterFunction<ServerResponse> buildServiceRoute(String pattern, String targetUrl) {
        return route()
            .route(path(pattern), http(targetUrl))
            .filter((req, next) -> {
                ServerRequest.Builder builder = ServerRequest.from(req);

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p) {
                    String jwt = jwtUtil.generateUserToken(p.getId(), p.getLogin(), p.getRole(), p.getPhoneNumber());
                    builder.header("Authorization", "Bearer " + jwt);
                }
                return next.handle(builder.build());
            })
            .build();
    }

    private RouterFunction<ServerResponse> buildPublicServiceRoute(String pattern, String targetUrl) {
        return route()
            .route(path(pattern), http(targetUrl))
            .build();
    }
}
