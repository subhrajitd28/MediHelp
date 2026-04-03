package com.medihelp.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DrugInteractionResponse {
    private String drug1;
    private String drug2;
    private String severity;
    private String description;
    private String source;
}
