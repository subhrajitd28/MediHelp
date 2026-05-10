package com.medihelp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    private String phone;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    // Cultural / chatbot context — collected on the registration form,
    // forwarded to user-service via UserRegisteredEvent so UserProfile is
    // populated atomically when the user is created.
    private java.time.LocalDate dateOfBirth;
    private String gender;
    private String state;
    private String dietPreference;
}
