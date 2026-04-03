package com.medihelp.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {
    @Size(max = 100)
    private String firstName;
    @Size(max = 100)
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodType;
    private Double height;
    private Double weight;
    private String profilePictureUrl;
    @Size(max = 500)
    private String bio;
}
