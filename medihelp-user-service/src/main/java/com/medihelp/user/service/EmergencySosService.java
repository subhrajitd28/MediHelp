package com.medihelp.user.service;

import com.medihelp.common.event.EmergencySosEvent;
import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.user.dto.SosResponse;
import com.medihelp.user.entity.*;
import com.medihelp.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private final RabbitTemplate rabbitTemplate;

    public SosResponse triggerSos(UUID userId) {
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

        return SosResponse.builder()
                .message("Emergency SOS sent to " + contacts.size() + " contact(s)")
                .contactsNotified(contacts.size())
                .contactNames(contacts.stream().map(EmergencyContact::getName).toList())
                .triggeredAt(Instant.now())
                .build();
    }
}
