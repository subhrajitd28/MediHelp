package com.medihelp.user.service;

import com.medihelp.common.event.EmergencySosEvent;
import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.user.dto.SosRequest;
import com.medihelp.user.dto.SosResponse;
import com.medihelp.user.entity.*;
import com.medihelp.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencySosService {

    private final UserProfileRepository profileRepository;
    private final EmergencyContactRepository contactRepository;
    private final AllergyRepository allergyRepository;
    private final HealthConditionRepository conditionRepository;
    private final SosShareableLinkRepository linkRepository;
    private final RabbitTemplate rabbitTemplate;

    public SosResponse triggerSos(UUID userId, SosRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        List<EmergencyContact> contacts = contactRepository.findByUserId(userId);
        List<Allergy> allergies = allergyRepository.findByUserId(userId);
        List<HealthCondition> conditions = conditionRepository.findByUserId(userId);

        if (contacts.isEmpty()) {
            return SosResponse.builder()
                    .message("No emergency contacts configured. Please add contacts in your profile.")
                    .contactsNotified(0)
                    .triggeredAt(Instant.now())
                    .build();
        }

        String userName = profile != null
                ? (profile.getFirstName() != null ? profile.getFirstName() : "") + " " + (profile.getLastName() != null ? profile.getLastName() : "")
                : "MediHelp User";
        String bloodType = profile != null ? profile.getBloodType() : "Unknown";

        List<String> allergyNames = allergies.stream().map(a -> a.getAllergen() + " (" + a.getSeverity() + ")").toList();
        List<String> conditionNames = conditions.stream().map(HealthCondition::getConditionName).toList();

        // Build medical summary HTML
        StringBuilder html = new StringBuilder();
        html.append("<h2 style='color:red;'>EMERGENCY SOS - ").append(userName).append("</h2>");
        html.append("<p><strong>Blood Type:</strong> ").append(bloodType != null ? bloodType : "Not specified").append("</p>");
        if (!allergyNames.isEmpty()) {
            html.append("<p><strong>Allergies:</strong> ").append(String.join(", ", allergyNames)).append("</p>");
        }
        if (!conditionNames.isEmpty()) {
            html.append("<p><strong>Medical Conditions:</strong> ").append(String.join(", ", conditionNames)).append("</p>");
        }
        if (request != null && request.getLatitude() != null && request.getLongitude() != null) {
            html.append("<p><strong>Location:</strong> <a href='https://www.google.com/maps?q=")
                    .append(request.getLatitude()).append(",").append(request.getLongitude())
                    .append("'>View on Google Maps</a></p>");
        }
        html.append("<p><strong>Time:</strong> ").append(Instant.now()).append("</p>");
        html.append("<p style='color:red;'><em>This person has triggered an emergency alert via MediHelp. Please check on them immediately.</em></p>");

        List<EmergencySosEvent.ContactInfo> contactInfos = contacts.stream()
                .map(c -> EmergencySosEvent.ContactInfo.builder()
                        .name(c.getName()).email(c.getEmail()).phone(c.getPhone()).build())
                .toList();

        EmergencySosEvent event = EmergencySosEvent.builder()
                .userId(userId.toString())
                .userName(userName.trim())
                .bloodType(bloodType)
                .allergies(allergyNames)
                .conditions(conditionNames)
                .medications(List.of())
                .medicalSummaryHtml(html.toString())
                .emergencyContacts(contactInfos)
                .triggeredAt(Instant.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_EMERGENCY_SOS, event);
            log.warn("EMERGENCY SOS triggered by user {} - notifying {} contacts", userId, contacts.size());
        } catch (Exception e) {
            log.error("Failed to publish SOS event: {}", e.getMessage());
        }

        // Generate shareable link (24h expiry)
        String token = generateSecureToken();
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        SosShareableLink link = SosShareableLink.builder()
                .token(token)
                .userId(userId)
                .medicalSummaryHtml(html.toString())
                .latitude(request != null ? request.getLatitude() : null)
                .longitude(request != null ? request.getLongitude() : null)
                .expiresAt(expiresAt)
                .build();
        linkRepository.save(link);

        String shareableUrl = "/api/v1/public/sos/" + token;

        return SosResponse.builder()
                .message("Emergency SOS sent to " + contacts.size() + " contact(s)")
                .contactsNotified(contacts.size())
                .contactNames(contacts.stream().map(EmergencyContact::getName).toList())
                .shareableLink(shareableUrl)
                .expiresAt(expiresAt)
                .triggeredAt(Instant.now())
                .build();
    }

    public String getShareableSummary(String token) {
        return linkRepository.findByToken(token)
                .filter(link -> link.getExpiresAt().isAfter(Instant.now()))
                .map(SosShareableLink::getMedicalSummaryHtml)
                .orElse(null);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
