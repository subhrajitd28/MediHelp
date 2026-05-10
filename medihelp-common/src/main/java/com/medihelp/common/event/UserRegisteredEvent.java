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
public class UserRegisteredEvent implements Serializable {

    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private Instant registeredAt;

    // Cultural / chatbot context — captured at registration so the user-service
    // can populate UserProfile in the listener; chatbot reads it via /users/me
    private java.time.LocalDate dateOfBirth;
    private String gender;
    private String state;
    private String dietPreference;
}
