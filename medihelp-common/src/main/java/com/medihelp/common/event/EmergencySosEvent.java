package com.medihelp.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencySosEvent implements Serializable {

    private String userId;
    private String userName;
    private String bloodType;
    private List<String> allergies;
    private List<String> conditions;
    private List<String> medications;
    private String medicalSummaryHtml;
    private List<ContactInfo> emergencyContacts;
    private Instant triggeredAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo implements Serializable {
        private String name;
        private String email;
        private String phone;
    }
}
