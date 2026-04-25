package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorFeedback {

    private ExecutorStatus status;
    private String summary;
    private List<String> evidence = new ArrayList<>();
    private String error;
    private String nextHint;
}
