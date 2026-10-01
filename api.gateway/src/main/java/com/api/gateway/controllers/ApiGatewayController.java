package com.api.gateway.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import com.common.dto.demo_hotel_service_dto.GeneralBookingDto;
import com.common.dto.demo_hotel_service_dto.RoomUnitDto;
import com.common.dto.demo_resource_service_dto.PhysicalServiceUnitDto;
import com.common.dto.demo_hotel_service_dto.SpaUnitDto;
import com.common.dto.demo_resource_service_dto.SpaWorkerDto;
import com.common.security.AuthPrincipal;
import com.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ApiGatewayController {
    private final RestClient restClient = RestClient.create();
    private final JwtUtil jwtUtil;

    @Value("${booking_service_url}")
    private String bookingServiceUrl;
    @Value("${resource_service_url}")
    private String resourceServiceUrl;

    @GetMapping("/home")
    public String home() {
        return "booking/home.html";
    }

    @GetMapping("/auth")
    public String auth(@AuthenticationPrincipal AuthPrincipal userDetails, Model model) {
        if (userDetails != null) {
            // якщо користувач вже залогінений, перенаправляємо його на /home
            return "redirect:" + "/home";
        }
        model.addAttribute("isAuthPage", true);
        return "auth/auth.html";
    }

    @GetMapping("/about-us")
    public String about_us() {
        return "common/about-us.html";
    }

    @GetMapping("/services")
    public String getServicesPage() {
        return "booking/services.html";
    }

    @GetMapping("/bookings-submit")
    public String bookingSubmit(Model model) {
        model.addAttribute("isCheckoutPage", true);
        return "booking/bookings-submit.html";
    }

    @GetMapping("/bookings")
    public String getBookingsPage(Model model, @AuthenticationPrincipal AuthPrincipal userDetails) {
        // перевірка авторизації
        if (userDetails == null) {
            model.addAttribute("showLoginOverlay", true);
            return "booking/bookings.html";
        }

        String token = jwtUtil.generateUserToken(userDetails.getId(), userDetails.getLogin(), userDetails.getRole(), userDetails.getPhoneNumber());
        ParameterizedTypeReference<List<GeneralBookingDto>> responseType = 
                new ParameterizedTypeReference<List<GeneralBookingDto>>() {};

        List<GeneralBookingDto> myBookings = restClient.get()
                .uri(bookingServiceUrl + "/api/bookings?page=0&size=12&showAll=false")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(responseType);
        model.addAttribute("myBookings", myBookings);

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            List<GeneralBookingDto> allBookings = restClient.get()
                    .uri(bookingServiceUrl + "/api/admin/bookings?page=0&size=12&showAll=false") 
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(responseType);
            model.addAttribute("allBookings", allBookings);
        }

        model.addAttribute("showLoginOverlay", false);
        return "booking/bookings.html";
    }

    @GetMapping("/rooms")
    public String rooms(Model model, @AuthenticationPrincipal AuthPrincipal userDetails) {
        ParameterizedTypeReference<List<RoomUnitDto>> responseType = 
            new ParameterizedTypeReference<List<RoomUnitDto>>() {};

        var request = restClient.get()
                .uri(bookingServiceUrl + "/api/rooms?page=0&size=12");

        if (userDetails != null) {
            request.header("Authorization", "Bearer " + jwtUtil.generateUserToken(
                    userDetails.getId(), userDetails.getLogin(), userDetails.getRole(), userDetails.getPhoneNumber()));
        }

        List<RoomUnitDto> rooms = request.retrieve().body(responseType);

        model.addAttribute("rooms", rooms);
        return "booking/rooms.html";
    }

    @GetMapping("/spas")
    public String spas(Model model, @AuthenticationPrincipal AuthPrincipal userDetails) {
        ParameterizedTypeReference<List<SpaUnitDto>> responseType = 
            new ParameterizedTypeReference<List<SpaUnitDto>>() {};

        var request = restClient.get().uri(bookingServiceUrl + "/api/spas?page=0&size=12");

        if (userDetails != null) {
            request.header("Authorization", "Bearer " + jwtUtil.generateUserToken(
                    userDetails.getId(), userDetails.getLogin(), userDetails.getRole(), userDetails.getPhoneNumber()));
        }

        // Jackson сам створить екземпляри SpaUnitDto
        List<SpaUnitDto> spas = request.retrieve().body(responseType);

        model.addAttribute("spas", spas);
        return "booking/spas.html";
    }

    @GetMapping("/admin/resources-and-staff-management")
    public String adminInventory(Model model, @AuthenticationPrincipal AuthPrincipal userDetails) {
        String token = jwtUtil.generateUserToken(userDetails.getId(), userDetails.getLogin(), userDetails.getRole(), userDetails.getPhoneNumber());

        ParameterizedTypeReference<List<PhysicalServiceUnitDto>> physicalUnitsType = new ParameterizedTypeReference<>() {};
        List<PhysicalServiceUnitDto> physicalUnits = restClient.get()
                .uri(resourceServiceUrl + "/api/physical-units?page=0&size=4") 
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(physicalUnitsType);

        ParameterizedTypeReference<List<SpaWorkerDto>> workersType = new ParameterizedTypeReference<>() {};
        List<SpaWorkerDto> spaWorkers = restClient.get()
                .uri(resourceServiceUrl + "/api/spa-workers?page=0&size=4")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(workersType);

        model.addAttribute("physicalUnits", physicalUnits);
        model.addAttribute("spaWorkers", spaWorkers);
        
        return "inventory/resources-and-staff-management.html";
    }

    

}
