package com.medihelp.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${RESEND_API_KEY:not-set}")
    private String resendApiKey;

    private static final String RESEND_URL = "https://api.resend.com/emails";
    private static final String FROM_EMAIL = "MediHelp <onboarding@resend.dev>";

    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "MediHelp - Your Verification Code: " + otp;
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                    <h2 style="color: #3f51b5;">MediHelp</h2>
                    <p>Your verification code is:</p>
                    <div style="background: #f5f5f5; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #3f51b5;">%s</span>
                    </div>
                    <p>This code expires in <strong>5 minutes</strong>.</p>
                    <p style="color: #999; font-size: 12px;">If you didn't request this code, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee;">
                    <p style="color: #999; font-size: 11px;">MediHelp - Your Personal Health Assistant</p>
                </div>
                """.formatted(otp);

        sendEmail(toEmail, subject, html);
    }

    public void sendPasswordResetEmail(String toEmail, String otp) {
        String subject = "MediHelp - Password Reset Code: " + otp;
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                    <h2 style="color: #3f51b5;">MediHelp</h2>
                    <p>You requested a password reset. Your code is:</p>
                    <div style="background: #f5f5f5; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #e91e63;">%s</span>
                    </div>
                    <p>This code expires in <strong>5 minutes</strong>.</p>
                    <p style="color: #999; font-size: 12px;">If you didn't request this, your account is safe — just ignore this email.</p>
                </div>
                """.formatted(otp);

        sendEmail(toEmail, subject, html);
    }

    private void sendEmail(String to, String subject, String html) {
        if ("not-set".equals(resendApiKey) || resendApiKey == null || resendApiKey.isBlank()) {
            log.info("EMAIL (no API key) >> To: {}, Subject: {}", to, subject);
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
                    "html", html
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = rest.postForEntity(RESEND_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP email sent via Resend to {}", to);
            } else {
                log.warn("Resend API returned {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send email via Resend to {}: {}", to, e.getMessage());
        }
    }
}
