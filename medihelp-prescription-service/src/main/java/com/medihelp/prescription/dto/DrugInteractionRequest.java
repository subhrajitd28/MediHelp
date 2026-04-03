package com.medihelp.prescription.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteractionRequest {

    @NotEmpty(message = "At least one drug name is required")
    private List<String> drugNames;
}
