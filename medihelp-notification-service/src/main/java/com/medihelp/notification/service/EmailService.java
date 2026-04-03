package com.medihelp.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-key:not-set}")
    private String resendApiKey;

    private static final String RESEND_URL = "https://api.resend.com/emails";
    private static final String FROM_EMAIL = "MediHelp <onboarding@resend.dev>";

    public void sendEmail(String to, String subject, String body) {
        if ("not-set".equals(resendApiKey) || resendApiKey == null || resendApiKey.isBlank()) {
            log.info("EMAIL (no API key, logging only) >> To: {}, Subject: {}", to, subject);
            return;
        }

        try {
            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, String> payload = Map.of(
                    "from", FROM_EMAIL,
                    "to", to,
                    "subject", subject,
                    "html", "<div style=\"font-family: Arial, sans-serif;\">"
                            + "<h2 style=\"color: #3f51b5;\">MediHelp</h2>"
                            + "<p>" + body.replace("\n", "<br>") + "</p>"
                            + "<hr><p style=\"color: #999; font-size: 12px;\">This is an automated message from MediHelp.</p>"
                            + "</div>"
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = rest.postForEntity(RESEND_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent via Resend to {} - Subject: {}", to, subject);
            } else {
                log.warn("Resend API returned {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send email via Resend to {}: {}", to, e.getMessage());
        }
    }
}
