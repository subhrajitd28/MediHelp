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
public class AppointmentReminderEvent implements Serializable {

    private String userId;
    private String appointmentId;
    private String doctorName;
    private String hospital;
    private String purpose;
    private Instant scheduledAt;
}
