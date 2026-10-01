package com.demo_user_service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;

/*
Важливо для правильної роботи надсилання смс:

Необхідно підключити телефон через usb до ПК -> Передавання файлів
Далі скачати android-sms-gateway на свій телефон: https://github.com/capcom6/android-sms-gateway/releases/tag/v1.61.1

Змінити значення в файлі application.properties на відповідні в додатку android-sms-gateway:
sms.gateway.url=http://127.0.0.1:8080/messages (змінити на відповідний порт)
sms.gateway.username= ваш username
sms.gateway.password= ваш пароль

далі виконати команду
adb forward tcp:8080 tcp:8080

якщо виводить adb.exe: no devices/emulators found - переконайтеся що в параметрах розорбника вашого телефону ввімкнено налагодження USB
*/

@Service
public class AdbSmsSenderService {

    private static final Logger log = LoggerFactory.getLogger(AdbSmsSenderService.class);
    private final RestClient restClient;

    public AdbSmsSenderService(
            RestClient.Builder restClientBuilder,
            @Value("${sms.gateway.url}") String gatewayUrl,
            @Value("${sms.gateway.username}") String username,
            @Value("${sms.gateway.password}") String password) {

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        this.restClient = restClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void sendSms(String phoneNumber, String messageText) {
        try {
            SmsRequest requestPayload = new SmsRequest(messageText, List.of(phoneNumber));

            log.info("НАДІСЛАНИЙ КОД: {}", messageText);

            ResponseEntity<Void> response = restClient.post()
                    .body(requestPayload)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Запит на відправку SMS успішно прийнято для номера: {}", phoneNumber);
            } else {
                log.error("Помилка відправки SMS! HTTP Код: {}", response.getStatusCode());
            }

        } catch (RestClientException e) {
            log.error("Не вдалося з'єднатися з телефоном через кабель (ADB): {}", e.getMessage());
        }
    }

        private record SmsRequest(String message, List<String> phoneNumbers) {
    }

    // public void sendSms(String phoneNumber, String messageText) {
    // try {
    // SmsRequest requestPayload = new SmsRequest(messageText,
    // List.of(phoneNumber));

    // log.info("НАДІСЛАНИЙ КОД: " + messageText);
    // // ResponseEntity<Void> response = new ResponseEntity<>();
    // // restClient.post()
    // // .body(requestPayload)
    // // .retrieve()
    // // .toBodilessEntity();

    // // if (response.getStatusCode().is2xxSuccessful()) {
    // // log.info("Запит на відправку SMS успішно прийнято для номера: {}",
    // // phoneNumber);
    // // } else {
    // // log.error("Помилка відправки SMS! HTTP Код: {}",
    // // response.getStatusCode());
    // // }

    // } catch (RestClientException e) {
    // log.error("Не вдалося з'єднатися з телефоном через кабель (ADB): {}",
    // e.getMessage());
    // }
    // }


}