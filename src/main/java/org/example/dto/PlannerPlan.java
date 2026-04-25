package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlannerPlan {

    private PlannerDecision decision;
    private String step;
    private String tool;
    private String context;
    private String reason;
    private String report;
}
