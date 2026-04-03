package com.medihelp.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReminderEvent implements Serializable {

    private String userId;
    private String medicationId;
    private String drugName;
    private String dosage;
    private Instant scheduledTime;
    private boolean withFood;
}
